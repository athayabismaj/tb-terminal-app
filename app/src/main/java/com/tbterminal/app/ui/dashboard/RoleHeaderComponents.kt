package com.tbterminal.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private val TopBarBrandGreen = Color(0xFF10533C)
private val TopBarSheetGreen = Color(0xFF1B5E3C)
private val TopBarSlate900 = Color(0xFF0F172A)
private val TopBarSlate700 = Color(0xFF334155)
private val TopBarSlate600 = Color(0xFF475569)
private val TopBarSlate500 = Color(0xFF64748B)
private val TopBarSlate400 = Color(0xFF94A3B8)
private val TopBarSlate300 = Color(0xFFCBD5E1)
private val TopBarSlate200 = Color(0xFFE2E8F0)
private val TopBarSlate100 = Color(0xFFF1F5F9)
private val TopBarSlate50 = Color(0xFFF8FAFC)

/**
 * Item menu di dalam dropdown profil pada top bar berbasis role.
 */
data class RoleHeaderMenuItem(
    val icon: ImageVector,
    val label: String,
    val textColor: Color,
    val iconColor: Color,
    val iconBgColor: Color,
    val showDividerBefore: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Top bar terpadu bergaya minimalis modern (seragam dengan header Kasir):
 * badge "TB" atau tombol kembali di kiri, judul + subjudul 2 baris, tombol aksi halaman,
 * dan tombol pil profil dengan popover animasi yang menyesuaikan role & kebutuhan halaman.
 */
@Composable
fun BrandedRoleTopBar(
    displayName: String,
    roleSubtitle: String,
    statusBadgeText: String,
    title: String,
    subtitle: String,
    menuItems: List<RoleHeaderMenuItem>,
    modifier: Modifier = Modifier,
    defaultInitial: String = "T",
    onBackClick: (() -> Unit)? = null,
    onBrandClick: (() -> Unit)? = null,
    pillAvatarBgColor: Color = TopBarSlate200,
    pillAvatarTextColor: Color = TopBarSlate700,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var lastDismissTime by remember { mutableLongStateOf(0L) }
    val menuTransitionState = remember { MutableTransitionState(false) }
    menuTransitionState.targetState = expanded

    val infiniteTransition = rememberInfiniteTransition(label = "TopBarPulseTransition")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "TopBarDotAlpha",
    )

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
        ),
        label = "ChevronRotation",
    )

    val cardScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.95f,
        animationSpec = tween(
            durationMillis = 200,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
        ),
        label = "UserCardScale",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = 180,
            easing = LinearEasing,
        ),
        label = "UserCardAlpha",
    )

    val userInitial = remember(displayName, defaultInitial) {
        displayName.trim().firstOrNull()?.uppercase() ?: defaultInitial
    }

    val density = LocalDensity.current
    val popupYOffset = remember(density) { with(density) { 46.dp.roundToPx() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Sisi Kiri: Tombol Kembali (child screen) / Badge TB (root screen) + Title & Subtitle
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onBackClick != null) {
                    Surface(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(11.dp),
                        color = TopBarSlate50,
                        border = BorderStroke(1.dp, TopBarSlate200),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("topbar-back-button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Kembali",
                                tint = TopBarSlate900,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(TopBarBrandGreen)
                            .then(
                                if (onBrandClick != null) {
                                    Modifier.clickable(onClick = onBrandClick)
                                } else {
                                    Modifier
                                },
                            )
                            .testTag("topbar-brand-badge"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "TB",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy((-1).dp),
                ) {
                    Text(
                        text = title,
                        color = TopBarSlate900,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 18.sp,
                    )
                    Text(
                        text = subtitle,
                        color = TopBarSlate500,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.15).sp,
                        lineHeight = 14.sp,
                    )
                }
            }

            // Sisi Kanan: Optional Actions + User Profile Pill + Popover Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (actions != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        actions()
                    }
                }

                Box {
                    Surface(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastDismissTime > 250L) {
                                expanded = !expanded
                            }
                        },
                        shape = RoundedCornerShape(100.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (expanded) TopBarBrandGreen else TopBarSlate200,
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("topbar-profile-pill"),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(pillAvatarBgColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = userInitial,
                                    color = pillAvatarTextColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = "Menu akun pengguna",
                                tint = if (expanded) TopBarBrandGreen else TopBarSlate500,
                                modifier = Modifier
                                    .size(14.dp)
                                    .graphicsLayer { rotationZ = chevronRotation },
                            )
                        }
                    }

                    if (menuTransitionState.currentState || menuTransitionState.targetState) {
                        Popup(
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(x = 0, y = popupYOffset),
                            onDismissRequest = {
                                lastDismissTime = System.currentTimeMillis()
                                expanded = false
                            },
                            properties = PopupProperties(focusable = true),
                        ) {
                            AnimatedVisibility(
                                visibleState = menuTransitionState,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
                                    ),
                                ) + slideInVertically(
                                    initialOffsetY = { with(density) { (-8).dp.roundToPx() } },
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
                                    ),
                                ) + scaleIn(
                                    initialScale = 0.98f,
                                    transformOrigin = TransformOrigin(1f, 0f),
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
                                    ),
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
                                    ),
                                ) + slideOutVertically(
                                    targetOffsetY = { with(density) { (-8).dp.roundToPx() } },
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
                                    ),
                                ) + scaleOut(
                                    targetScale = 0.98f,
                                    transformOrigin = TransformOrigin(1f, 0f),
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
                                    ),
                                ),
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .width(264.dp)
                                        .shadow(
                                            elevation = 16.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            ambientColor = Color(0x1A0F172A),
                                            spotColor = Color(0x260F172A),
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, TopBarSlate100),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        // Identity Card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    scaleX = cardScale
                                                    scaleY = cardScale
                                                    alpha = cardAlpha
                                                }
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(TopBarSlate50)
                                                .border(1.dp, TopBarSlate100, RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE8F3EC)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = userInitial,
                                                        color = TopBarSheetGreen,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = displayName,
                                                        color = TopBarSlate900,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = roleSubtitle,
                                                        color = TopBarSlate500,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        letterSpacing = 0.4.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(Color(0xFFECFDF5))
                                                            .border(
                                                                1.dp,
                                                                Color(0xFFA7F3D0).copy(alpha = 0.6f),
                                                                RoundedCornerShape(999.dp),
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFF10B981).copy(alpha = dotAlpha)),
                                                            )
                                                            Text(
                                                                text = statusBadgeText,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color(0xFF047857),
                                                                maxLines = 1,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        menuItems.forEach { item ->
                                            if (item.showDividerBefore) {
                                                HorizontalDivider(
                                                    color = TopBarSlate100,
                                                    thickness = 1.dp,
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                )
                                            }
                                            RoleHeaderDropdownItem(
                                                icon = item.icon,
                                                label = item.label,
                                                textColor = item.textColor,
                                                iconColor = item.iconColor,
                                                iconBgColor = item.iconBgColor,
                                                onClick = {
                                                    expanded = false
                                                    item.onClick()
                                                },
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
        HorizontalDivider(color = TopBarSlate100, thickness = 1.dp)
    }
}

@Composable
fun RoleHeaderDropdownItem(
    icon: ImageVector,
    label: String,
    textColor: Color,
    iconColor: Color,
    iconBgColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Sheet info toko & sistem untuk role Admin dan Owner (senada dengan CashierTerminalInfoModalSheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackofficeSystemInfoModalSheet(
    userName: String,
    role: String,
    onSecondaryActionClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isOwner = isOwnerPersona(role)
    val displayName = remember(userName, isOwner) {
        val trimmed = userName.trim()
        when {
            trimmed.isEmpty() -> if (isOwner) "Pemilik Toko" else "Admin"
            isOwner && (trimmed.equals("owner", ignoreCase = true) || trimmed.equals("pemilik", ignoreCase = true)) -> "Pemilik Toko"
            !isOwner && trimmed.equals("admin", ignoreCase = true) -> "Admin"
            else -> trimmed
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TopBarSlate200),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF4F9F5))
                            .border(1.dp, TopBarSheetGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "TB",
                            color = TopBarSheetGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                        )
                    }
                    Column {
                        Text(
                            text = if (isOwner) "Ringkasan Sistem & Toko" else "Detail Operasional Toko",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TopBarSlate900,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                            )
                            Text(
                                text = if (isOwner) "Online · Kendali Eksekutif" else "Online · Sinkronisasi Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857),
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TopBarSlate100),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TopBarSlate500,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isOwner) "STATUS SERVER & BACKOFFICE" else "STATUS JARINGAN & OPERASIONAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TopBarSlate500,
                        letterSpacing = 0.5.sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "ONLINE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TopBarSheetGreen,
                        )
                        Text(
                            text = "· Tersambung Cloud Server",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TopBarSlate600,
                        )
                    }
                    Text(
                        text = if (isOwner) {
                            "Laporan bisnis, keuangan, gudang, dan log keamanan toko tersinkronisasi real-time."
                        } else {
                            "Data stok gudang, barang masuk, transaksi kasir, dan piutang tersinkronisasi otomatis."
                        },
                        fontSize = 11.sp,
                        color = TopBarSlate600,
                        lineHeight = 16.sp,
                    )
                }
            }

            // Detail Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BackofficeSystemDetailRow(
                    label = "Nama Toko",
                    value = "TB Barokah Jaya",
                    subValue = if (isOwner) "(Pusat)" else "(Cabang Utama)",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TopBarSlate50)
                            .border(1.dp, TopBarSlate200, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Column {
                            Text(
                                text = if (isOwner) "Peran Akses" else "Peran Akun",
                                fontSize = 11.sp,
                                color = TopBarSlate400,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOwner) "OWNER " else "ADMIN ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TopBarSlate900,
                                )
                                Text(
                                    text = if (isOwner) "(Penuh)" else "(Operasional)",
                                    fontSize = 10.sp,
                                    color = TopBarSlate500,
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TopBarSlate50)
                            .border(1.dp, TopBarSlate200, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    ) {
                        Column {
                            Text(
                                text = "Versi Aplikasi",
                                fontSize = 11.sp,
                                color = TopBarSlate400,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "v1.0.0 ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TopBarSlate900,
                                )
                                Text(
                                    text = "(Build 1)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857),
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TopBarSlate50)
                        .border(1.dp, TopBarSlate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                            )
                            Text(
                                text = if (isOwner) "Keamanan & Backup" else "Sinkronisasi Data",
                                fontSize = 12.sp,
                                color = TopBarSlate500,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOwner) "Audit & Cloud" else "Cloud & Lokal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TopBarSlate900,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (isOwner) "Aktif" else "Terhubung",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857),
                                )
                            }
                        }
                    }
                }

                BackofficeSystemDetailRow(
                    label = if (isOwner) "Pemilik Bertugas" else "Admin Bertugas",
                    value = displayName,
                    subValue = "(Aktif)",
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSecondaryActionClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TopBarSlate300),
                ) {
                    Icon(
                        imageVector = if (isOwner) Icons.Outlined.Assessment else Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TopBarSlate700,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOwner) "Buka Laporan Bisnis" else "Buka Pusat Sinkronisasi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TopBarSlate700,
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TopBarSheetGreen,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Tutup",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackofficeSystemDetailRow(
    label: String,
    value: String,
    subValue: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TopBarSlate50)
            .border(1.dp, TopBarSlate200, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TopBarSlate500,
                fontWeight = FontWeight.Medium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TopBarSlate900,
                )
                if (subValue.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subValue,
                        fontSize = 10.sp,
                        color = TopBarSlate500,
                    )
                }
            }
        }
    }
}
