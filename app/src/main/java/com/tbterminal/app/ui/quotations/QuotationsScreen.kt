package com.tbterminal.app.ui.quotations

import com.tbterminal.app.ui.components.TbPagination

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.QuotationDetail
import com.tbterminal.app.data.model.QuotationStatus
import com.tbterminal.app.data.model.QuotationSummary
import com.tbterminal.app.data.repository.QuotationRepository
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.components.TopFloatingCapsule
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

// ═════════════════════════════════════════════════════════════════════════════
// INDUSTRIAL POS MODERN MINIMAL DESIGN TOKENS
// ═════════════════════════════════════════════════════════════════════════════
private val BrandGreen = Color(0xFF10533C)
private val BrandGreenLight = Color(0xFFE8F3EC)

private val ScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color.White
private val SurfaceMuted = Color(0xFFF1F5F9)

private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF475569)
private val TextMuted = Color(0xFF94A3B8)
private val BorderLight = Color(0xFFE2E8F0)

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

private fun isQuotationExpired(validUntilIso: String?): Boolean {
    if (validUntilIso.isNullOrBlank()) return false
    return runCatching {
        val expiryDate = runCatching {
            OffsetDateTime.parse(validUntilIso).atZoneSameInstant(WIB_ZONE).toLocalDate()
        }.recoverCatching {
            Instant.parse(validUntilIso).atZone(WIB_ZONE).toLocalDate()
        }.recoverCatching {
            LocalDate.parse(validUntilIso.take(10))
        }.getOrNull() ?: return false

        LocalDate.now(WIB_ZONE).isAfter(expiryDate)
    }.getOrDefault(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationsScreen(
    role: String,
    quotationRepository: QuotationRepository,
    storeName: String = "TB TERMINAL",
    onBackClick: () -> Unit,
    onConvertToTransaction: (QuotationDetail) -> Unit,
    viewModel: QuotationsViewModel = viewModel(
        factory = QuotationsViewModel.factory(quotationRepository)
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val canConvertToPos = AppAccessPolicy.can(role, AppCapability.POS)

    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by remember { mutableStateOf(false) }

    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()
    val isFiltered = uiState.selectedStatus != ALL_QUOTATION_STATUSES

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Header Bar (Back, Title, Action Icons) - Background putih persis Keranjang
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sisi Kiri: Tombol Back + Judul "Penawaran (RAB)" persis gaya Keranjang
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "Penawaran (RAB)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.3).sp
                            )
                        }

                        // Sisi Kanan: Action Icons (Search, Filter) - Tanpa bg card saat diklik/aktif
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Search Button
                            IconButton(
                                onClick = {
                                    if (isSearchActive) {
                                        isSearchExpanded = false
                                        viewModel.onSearchChanged("")
                                    } else {
                                        isSearchExpanded = true
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("quotations-toggle-search")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Cari penawaran",
                                    tint = if (isSearchActive) BrandGreen else Color(0xFF475569),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Filter Button
                            Box(
                                modifier = Modifier.size(34.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { showFilterPopup = true },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("quotations-open-filters")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "Filter status penawaran",
                                        tint = if (isFiltered) BrandGreen else Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (isFiltered) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 3.dp, end = 3.dp)
                                            .size(7.dp)
                                            .background(Color(0xFFE5A93C), CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Expandable Search Bar (Di belakangnya TIDAK ada kontainer, transparan menyatu ke bg utama) ───
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 12.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            "Cari nomor RAB atau nama pelanggan...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = viewModel::onSearchChanged,
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color(0xFF0F172A),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("quotations-search-field"),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            viewModel.onSearchChanged("")
                                        } else {
                                            isSearchExpanded = false
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "Tutup pencarian",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            TopFloatingCapsule(
                message = uiState.actionMessage,
                error = uiState.errorMessage,
                onDismiss = viewModel::clearActionMessage,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Quick Filter Bar (Tampil saat filter status aktif) ───────
                if (isFiltered) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Filter:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            val statusLabel = when (uiState.selectedStatus) {
                                QuotationStatus.DRAFT.name -> "Draft"
                                QuotationStatus.SENT.name -> "Terkirim"
                                QuotationStatus.CONVERTED.name -> "Jadi Transaksi"
                                QuotationStatus.CANCELLED.name -> "Dibatalkan"
                                else -> uiState.selectedStatus
                            }
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = statusLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.onStatusSelected(ALL_QUOTATION_STATUSES) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Reset",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF43F5E),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.onStatusSelected(ALL_QUOTATION_STATUSES) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                // ── Content State (Loading / Empty / List Standard Industri) ─
                when {
                    uiState.isLoading && uiState.quotations.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrandGreen, strokeWidth = 3.dp)
                        }
                    }
                    uiState.quotations.isEmpty() -> {
                        QuotationEmptyState(
                            hasFilterOrSearch = uiState.searchQuery.isNotBlank() || isFiltered,
                            onResetFilter = {
                                isSearchExpanded = false
                                viewModel.onSearchChanged("")
                                viewModel.onStatusSelected(ALL_QUOTATION_STATUSES)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Container Card Tunggal Berstandar Industri (Persis HTML Mockup)
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.9f)),
                                    shadowElevation = 0.5.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        uiState.quotations.forEachIndexed { index, summary ->
                                            if (index > 0) {
                                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                            }
                                            QuotationListItem(
                                                summary = summary,
                                                onClick = { viewModel.openDetail(summary.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Pagination Kapsul di Bawah
                            item {
                                QuotationPaginationCapsule(
                                    currentPage = uiState.page,
                                    totalPages = uiState.totalPages,
                                    totalItems = uiState.total,
                                    onPreviousPage = viewModel::previousPage,
                                    onNextPage = viewModel::nextPage,
                                    isLoading = uiState.isLoading
                                )
                            }
                        }
                    }
                }
            }

            // ── Detail Loading Overlay ──────────────────────────────────────
            if (uiState.isDetailLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = BrandGreen,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Memuat rincian penawaran...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Filter Bottom Sheet (Persis Gaya Penjualan) ─────────────────────────
    if (showFilterPopup) {
        QuotationFilterBottomSheet(
            selectedStatus = uiState.selectedStatus,
            onDismiss = { showFilterPopup = false },
            onApplyStatus = viewModel::onStatusSelected,
            onResetFilters = { viewModel.onStatusSelected(ALL_QUOTATION_STATUSES) }
        )
    }

    // ── Quotation Detail Bottom Sheet (Persis HTML Mockup) ───────────────────
    uiState.selectedDetail?.let { detail ->
        QuotationDetailBottomSheet(
            detail = detail,
            storeName = storeName,
            canConvertToPos = canConvertToPos,
            isMutating = uiState.isMutating,
            onDismiss = viewModel::dismissDetail,
            onPrintPdf = { printQuotationPdf(context, detail, storeName) },
            onShareText = {
                shareQuotationText(context, detail, storeName)
                if (detail.status == QuotationStatus.DRAFT) {
                    viewModel.updateStatus(detail.id, QuotationStatus.SENT)
                }
            },
            onCancel = { viewModel.updateStatus(detail.id, QuotationStatus.CANCELLED) },
            onConvertToTransaction = {
                viewModel.dismissDetail()
                onConvertToTransaction(detail)
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// QUOTATION LIST ITEM (INDUSTRY STANDARD B2B/RETAIL ERP & POS)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun QuotationListItem(
    summary: QuotationSummary,
    onClick: () -> Unit,
) {
    val isExpired = remember(summary.validUntil) { isQuotationExpired(summary.validUntil) }
    val isOverdue = isExpired && summary.status != QuotationStatus.CONVERTED && summary.status != QuotationStatus.CANCELLED
    val isCancelled = summary.status == QuotationStatus.CANCELLED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("quotation-card-${summary.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header Row: Quotation Number (font-mono) + Clean Status Text ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary.quotationNumber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.2).sp
            )

            val (statusLabel, statusColor) = when (summary.status) {
                QuotationStatus.DRAFT -> "Draft" to Color(0xFFB45309)
                QuotationStatus.SENT -> "Terkirim" to Color(0xFF1D4ED8)
                QuotationStatus.CONVERTED -> "Jadi Transaksi" to Color(0xFF047857)
                QuotationStatus.CANCELLED -> "Dibatalkan" to Color(0xFFE11D48)
            }

            Text(
                text = statusLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        // ── Details: Dibuat, Kasir, Pelanggan, Masa Berlaku ──
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Row 1: Dibuat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dibuat", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(
                    text = formatQuotationDate(summary.createdAt),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            }

            // Row 2: Kasir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kasir", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(
                    text = summary.cashierName ?: "-",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            }

            // Row 3: Pelanggan (with Storefront Icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pelanggan", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = summary.customerName?.takeIf { it.isNotBlank() } ?: "Pelanggan Umum",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            // Row 4: Masa Berlaku
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Masa Berlaku", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "s/d ${formatQuotationDate(summary.validUntil)}",
                        fontSize = 11.sp,
                        color = if (isOverdue) Color(0xFFE11D48) else Color(0xFF94A3B8)
                    )
                    if (isOverdue) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "Kadaluarsa",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Optional notes
            summary.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Catatan", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = notes,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        // ── Footer: Total Estimasi + Right Chevron ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTAL ESTIMASI (${summary.itemCount} BARANG)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = formatQuotationMoney(summary.total),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isCancelled) Color(0xFF334155) else BrandGreen,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// CENTERED PAGINATION CAPSULE (PERSIS SEPERTI MOCKUP)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun QuotationPaginationCapsule(
    currentPage: Int,
    totalPages: Int,
    totalItems: Long,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    isLoading: Boolean,
) {
    TbPagination(
        currentPage = currentPage,
        totalPages = totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = "$totalItems dokumen",
        isLoading = isLoading,
        testTag = "quotations-pagination",
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// QUOTATION FILTER BOTTOM SHEET (PERSIS SEPERTI SALES FILTER POP-UP)
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotationFilterBottomSheet(
    selectedStatus: String,
    onDismiss: () -> Unit,
    onApplyStatus: (String) -> Unit,
    onResetFilters: () -> Unit,
) {
    var tempSelectedStatus by remember { mutableStateOf(selectedStatus) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quotation-filter-sheet")
        ) {
            // Modal Header (Persis SalesFilterBottomSheet)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Filter Penawaran (RAB)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur status dokumen penawaran harga",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup Filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Content: Status Options (Persis SalesFilterBottomSheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "STATUS DOKUMEN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.8.sp,
                )

                val statusRows = listOf(
                    listOf(ALL_QUOTATION_STATUSES to "Semua Status", QuotationStatus.DRAFT.name to "Draft"),
                    listOf(QuotationStatus.SENT.name to "Terkirim", QuotationStatus.CONVERTED.name to "Jadi Transaksi")
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    statusRows.forEach { rowPairs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPairs.forEach { (key, label) ->
                                val isSelected = tempSelectedStatus == key
                                QuotationFilterOptionCard(
                                    label = label,
                                    isSelected = isSelected,
                                    onClick = { tempSelectedStatus = key },
                                    modifier = Modifier.weight(1f),
                                    testTag = "quotation-filter-option-$key"
                                )
                            }
                        }
                    }

                    // Full width status: Dibatalkan
                    val isCancelled = tempSelectedStatus == QuotationStatus.CANCELLED.name
                    QuotationFilterOptionCard(
                        label = "Dibatalkan",
                        isSelected = isCancelled,
                        onClick = { tempSelectedStatus = QuotationStatus.CANCELLED.name },
                        modifier = Modifier.fillMaxWidth(),
                        isCentered = true,
                        testTag = "quotation-filter-option-${QuotationStatus.CANCELLED.name}"
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Footer Action Buttons (Terapkan di atas solid, Batal di bawah text button persis filter penjualan)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        onApplyStatus(tempSelectedStatus)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quotation-filter-apply"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Text(
                        text = "Terapkan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("quotation-filter-cancel")
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotationFilterOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    isCentered: Boolean = false,
    icon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(46.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) BrandGreen else Color(0xFFE2E8F0)
        ),
        shadowElevation = 0.dp
    ) {
        if (isCentered) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) BrandGreen else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) BrandGreen else Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) BrandGreen else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) BrandGreen else Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// EMPTY STATE (CLEAN INDUSTRIAL POS STYLE)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun QuotationEmptyState(
    hasFilterOrSearch: Boolean,
    onResetFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SurfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = if (hasFilterOrSearch) "Penawaran Tidak Ditemukan" else "Belum Ada Penawaran Harga",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = if (hasFilterOrSearch) {
                    "Tidak ada penawaran RAB yang cocok dengan pencarian atau filter status yang dipilih."
                } else {
                    "Buat penawaran harga resmi langsung dari keranjang kasir dengan menekan tombol 'Simpan RAB'."
                },
                fontSize = 12.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp)
            )
            if (hasFilterOrSearch) {
                OutlinedButton(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Reset Pencarian & Filter", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// QUOTATION DETAIL BOTTOM SHEET (PERSIS SEPERTI MOCKUP DETAIL PENAWARAN RAB)
// ═════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotationDetailBottomSheet(
    detail: QuotationDetail,
    storeName: String,
    canConvertToPos: Boolean,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onPrintPdf: () -> Unit,
    onShareText: () -> Unit,
    onCancel: () -> Unit,
    onConvertToTransaction: () -> Unit,
) {
    val isEditable = detail.status == QuotationStatus.DRAFT || detail.status == QuotationStatus.SENT
    val isCancelled = detail.status == QuotationStatus.CANCELLED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quotation-detail-sheet")
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Detail Penawaran (RAB)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = detail.quotationNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup sheet",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Scrollable Sheet Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Info Toko & Pelanggan: Baris Bersih 2 Kolom ──
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Toko", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(storeName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kasir", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(detail.cashierName ?: "-", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pelanggan", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = detail.customerName?.takeIf { it.isNotBlank() } ?: "Pelanggan Umum",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.6f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tanggal Dibuat", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(formatQuotationDate(detail.createdAt), fontSize = 12.sp, color = Color(0xFF475569))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Masa Berlaku", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(formatQuotationDate(detail.validUntil), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                        }

                        detail.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                            HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.6f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                            Text(
                                text = "Catatan: $notes",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // ── Daftar Barang ──
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DAFTAR BARANG (${detail.items.size} ITEM)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        detail.items.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${item.quantity.stripTrailingZeros().toPlainString()} ${item.unitName ?: "pcs"} × ${formatQuotationMoney(item.unitPrice)}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    if (item.discountAmount > BigDecimal.ZERO) {
                                        Text(
                                            text = "Diskon item: -${formatQuotationMoney(item.discountAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                    item.note?.takeIf { it.isNotBlank() }?.let { note ->
                                        Text(
                                            text = "Ket: $note",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Text(
                                    text = formatQuotationMoney(item.subtotal),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // ── Ringkasan Biaya ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(formatQuotationMoney(detail.grossSubtotal), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                    }

                    if (detail.totalDiscountAmount > BigDecimal.ZERO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Diskon", fontSize = 12.sp, color = Color(0xFF047857))
                            Text("-${formatQuotationMoney(detail.totalDiscountAmount)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
                        }
                    }

                    if (detail.taxEnabled && detail.taxAmount > BigDecimal.ZERO) {
                        val taxTitle = "${detail.taxLabel ?: "PPN"} ${detail.taxRate.stripTrailingZeros().toPlainString()}%"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(taxTitle, fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(formatQuotationMoney(detail.taxAmount), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF334155))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.8f), thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "ESTIMASI TOTAL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Sudah termasuk pajak & diskon",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Text(
                            text = formatQuotationMoney(detail.total),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = BrandGreen,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                // ── Aksi Kasir Standar Industri ──
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Notice jika dibatalkan
                    if (isCancelled) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "RAB Dibatalkan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = "Tidak dapat diproses ke kasir POS",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Primary Action: Jadikan Transaksi (POS)
                    if (canConvertToPos && isEditable) {
                        Button(
                            onClick = onConvertToTransaction,
                            enabled = !isMutating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCartCheckout,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Proses Transaksi Kasir (POS)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Secondary Actions: WhatsApp & Cetak PDF (Grid 2 kolom)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShareText,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Chat,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        }

                        OutlinedButton(
                            onClick = onPrintPdf,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Cetak PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        }
                    }

                    // Cancel RAB option
                    if (isEditable) {
                        TextButton(
                            onClick = onCancel,
                            enabled = !isMutating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Batalkan Dokumen RAB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}
