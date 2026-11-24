package com.tbterminal.app.ui.dashboard.owner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseItemDetail
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.common.viewModelFactory
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerAmberInk
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val OWNER_DETAIL_PURCHASE_TABLET_BREAKPOINT = 840.dp
private val PURCHASE_DETAIL_WIB_ZONE = ZoneId.of("Asia/Jakarta")
private val DetailNotaLunasDotColor = Color(0xFF8FE3BD)
private val DetailNotaHutangDotColor = Color(0xFFF2C14E)

@Immutable
internal data class OwnerPurchaseDetailUiState(
    val purchase: PurchaseDetail? = null,
    val matchingPayable: SupplierPayable? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isNotFoundError: Boolean = false
)

@Immutable
internal data class OwnerPurchasePaymentBreakdown(
    val total: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val isUnpaid: Boolean,
    val dueDate: String?,
    val isDueOverdue: Boolean
)

/**
 * TODO: `PurchaseDetail` dari API belum mengembalikan `paymentMethod`, `paidAmount`, `remainingAmount`,
 * dan `dueDate` secara langsung. Saat ini status, sisa utang, dan jatuh tempo dicocokkan dari
 * `getPayables(supplierId)` berdasarkan `purchaseId`. Konfirmasikan jika API detail nota sudah
 * menyertakan field tersebut langsung (termasuk aturan untuk nota tanpa item nama produk).
 */
internal fun resolvePurchasePaymentBreakdown(
    purchase: PurchaseDetail,
    matchingPayable: SupplierPayable?,
    today: LocalDate = LocalDate.now(PURCHASE_DETAIL_WIB_ZONE)
): OwnerPurchasePaymentBreakdown {
    val safeTotal = purchase.total.coerceAtLeast(BigDecimal.ZERO)
    if (matchingPayable == null) {
        return OwnerPurchasePaymentBreakdown(
            total = safeTotal,
            paidAmount = safeTotal,
            remainingAmount = BigDecimal.ZERO,
            isUnpaid = false,
            dueDate = null,
            isDueOverdue = false
        )
    }
    val remaining = matchingPayable.remainingAmount.coerceIn(BigDecimal.ZERO, safeTotal)
    val paid = safeTotal.subtract(remaining).coerceAtLeast(BigDecimal.ZERO)
    val rawDue = matchingPayable.dueDate.takeIf { it.isNotBlank() }
    val dueLocalDate = rawDue?.let(::parsePurchaseDateToLocalDate)
    val isOverdue = remaining > BigDecimal.ZERO && dueLocalDate != null && dueLocalDate.isBefore(today)
    return OwnerPurchasePaymentBreakdown(
        total = safeTotal,
        paidAmount = paid,
        remainingAmount = remaining,
        isUnpaid = remaining > BigDecimal.ZERO,
        dueDate = rawDue,
        isDueOverdue = isOverdue
    )
}

internal class OwnerPurchaseDetailViewModel(
    private val purchasingRepository: PurchasingRepository,
    private val purchaseId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(OwnerPurchaseDetailUiState())
    val uiState: StateFlow<OwnerPurchaseDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isNotFoundError = false) }
            when (val result = purchasingRepository.getPurchaseById(purchaseId)) {
                is RepositoryResult.Success -> {
                    val detail = result.data
                    _uiState.update {
                        it.copy(purchase = detail, isLoading = false, errorMessage = null, isNotFoundError = false)
                    }
                    loadMatchingPayable(detail.supplierId, detail.id)
                }
                is RepositoryResult.Error -> {
                    val isNotFound = result.code.contains("NOT_FOUND", ignoreCase = true) ||
                        result.message.contains("tidak ditemukan", ignoreCase = true)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message, isNotFoundError = isNotFound)
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Detail nota gagal dimuat karena koneksi bermasalah.",
                            isNotFoundError = false
                        )
                    }
                }
            }
        }
    }

    private fun loadMatchingPayable(supplierId: String, purchaseId: String) {
        viewModelScope.launch {
            when (val payables = purchasingRepository.getPayables(page = 1, limit = 100, supplierId = supplierId)) {
                is RepositoryResult.Success -> {
                    val match = payables.data.data.firstOrNull { it.purchaseId == purchaseId }
                    _uiState.update { it.copy(matchingPayable = match) }
                }
                else -> Unit
            }
        }
    }

    companion object {
        fun factory(
            purchasingRepository: PurchasingRepository,
            purchaseId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            OwnerPurchaseDetailViewModel(purchasingRepository, purchaseId)
        }
    }
}

@Composable
internal fun OwnerDetailNotaPembelianRoute(
    purchaseId: String,
    purchasingRepository: PurchasingRepository,
    onBack: () -> Unit,
    onSupplierClick: ((String) -> Unit)? = null,
    onDashboardClick: () -> Unit = {},
    onProductsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: OwnerPurchaseDetailViewModel = viewModel(
        key = "owner-purchase-detail-$purchaseId",
        factory = OwnerPurchaseDetailViewModel.factory(purchasingRepository, purchaseId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val destinationNavigator = LocalAdminDestinationNavigator.current

    fun handleNavSelect(destination: DashboardNavDestination) {
        when (destination) {
            DashboardNavDestination.BERANDA ->
                destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
            DashboardNavDestination.TRANSAKSI ->
                destinationNavigator?.invoke(AdminDestination.TransactionsHub) ?: onBack()
            DashboardNavDestination.KEUANGAN ->
                destinationNavigator?.invoke(AdminDestination.FinanceHub) ?: onReceivablesClick()
            DashboardNavDestination.STOK ->
                destinationNavigator?.invoke(AdminDestination.StockHub) ?: onProductsClick()
            DashboardNavDestination.MENU ->
                destinationNavigator?.invoke(AdminDestination.MoreHub) ?: onSettingsClick()
        }
    }

    OwnerDetailNotaPembelianHost(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::loadDetail,
        onSupplierClick = onSupplierClick,
        onNavSelect = ::handleNavSelect
    )
}

@Composable
internal fun OwnerDetailNotaPembelianHost(
    uiState: OwnerPurchaseDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSupplierClick: ((String) -> Unit)?,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    val context = LocalContext.current
    var copiedInvoiceValue by remember { mutableStateOf<String?>(null) }

    fun handleCopyInvoice(rawInvoice: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Nomor Nota", rawInvoice))
        copiedInvoiceValue = rawInvoice
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OwnerDetailNotaPembelianScreen(
            uiState = uiState,
            onBack = onBack,
            onRetry = onRetry,
            onCopyInvoice = ::handleCopyInvoice,
            onSupplierClick = onSupplierClick,
            onNavSelect = onNavSelect
        )

        AppAlert(
            type = AlertType.Success,
            title = "Nomor nota disalin",
            message = copiedInvoiceValue.orEmpty(),
            visible = copiedInvoiceValue != null,
            onDismiss = { copiedInvoiceValue = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("detail-purchase-copy-toast")
        )
    }
}

@Composable
internal fun OwnerDetailNotaPembelianScreen(
    uiState: OwnerPurchaseDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCopyInvoice: (String) -> Unit,
    onSupplierClick: ((String) -> Unit)?,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_DETAIL_PURCHASE_TABLET_BREAKPOINT
        if (isTablet) {
            TabletDetailNotaLayout(
                uiState = uiState,
                onBack = onBack,
                onRetry = onRetry,
                onCopyInvoice = onCopyInvoice,
                onSupplierClick = onSupplierClick,
                onNavSelect = onNavSelect
            )
        } else {
            PhoneDetailNotaLayout(
                uiState = uiState,
                onBack = onBack,
                onRetry = onRetry,
                onCopyInvoice = onCopyInvoice,
                onSupplierClick = onSupplierClick
            )
        }
    }
}

@Composable
private fun PhoneDetailNotaLayout(
    uiState: OwnerPurchaseDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCopyInvoice: (String) -> Unit,
    onSupplierClick: ((String) -> Unit)?
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val viewportHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
        val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Layout(
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brand)
                            .clipToBounds()
                    ) {
                        Box(modifier = Modifier.matchParentSize()) {
                            RingsPattern(
                                radii = listOf(50.dp, 95.dp, 140.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                                    .offset(x = 90.dp, y = (-90).dp)
                                    .size(300.dp)
                            )

                            DotsPattern(
                                dotRadius = 1.5.dp,
                                step = 20.dp,
                                maskAtBottomRight = true,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(width = 190.dp, height = 110.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 56.dp)
                        ) {
                            DetailNotaTopBar(onBack = onBack)

                            Spacer(modifier = Modifier.height(16.dp))

                            PurchaseDetailHeroHeader(
                                purchase = uiState.purchase,
                                matchingPayable = uiState.matchingPayable,
                                isLoading = uiState.isLoading,
                                onCopyInvoice = onCopyInvoice,
                                isTablet = false
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(SurfaceWhite)
                            .navigationBarsPadding()
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp)
                    ) {
                        DetailNotaSheetContent(
                            uiState = uiState,
                            onRetry = onRetry,
                            onSupplierClick = onSupplierClick
                        )
                    }
                }
            ) { measurables, constraints ->
                val headerPlaceable = measurables[0].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val minPanelHeight = (viewportHeightPx - (headerPlaceable.height - overlapPx)).coerceAtLeast(0)
                val panelPlaceable = measurables[1].measure(
                    constraints.copy(minHeight = minPanelHeight, maxHeight = Constraints.Infinity)
                )
                val totalHeight = (headerPlaceable.height - overlapPx + panelPlaceable.height)
                    .coerceAtLeast(headerPlaceable.height)
                layout(constraints.maxWidth, totalHeight) {
                    headerPlaceable.placeRelative(0, 0)
                    panelPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                }
            }
        }
    }
}

@Composable
private fun TabletDetailNotaLayout(
    uiState: OwnerPurchaseDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCopyInvoice: (String) -> Unit,
    onSupplierClick: ((String) -> Unit)?,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.TRANSAKSI,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(432.dp)
                    .fillMaxHeight()
                    .background(Brand)
                    .clipToBounds()
            ) {
                RingsPattern(
                    radii = listOf(50.dp, 95.dp, 140.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                        .offset(x = 90.dp, y = (-90).dp)
                        .size(300.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, top = 40.dp, end = 56.dp, bottom = 40.dp)
                ) {
                    DetailNotaTopBar(onBack = onBack)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        PurchaseDetailHeroHeader(
                            purchase = uiState.purchase,
                            matchingPayable = uiState.matchingPayable,
                            isLoading = uiState.isLoading,
                            onCopyInvoice = onCopyInvoice,
                            isTablet = true
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp)
            ) {
                DetailNotaSheetContent(
                    uiState = uiState,
                    onRetry = onRetry,
                    onSupplierClick = onSupplierClick
                )
            }
        }
    }
}

@Composable
private fun DetailNotaTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val title = "Detail nota pembelian"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val startOutsetPx = 12.dp.roundToPx()
                val expandedWidth = if (constraints.hasBoundedWidth) {
                    constraints.maxWidth + startOutsetPx
                } else {
                    constraints.maxWidth
                }
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = if (constraints.hasFixedWidth) expandedWidth else 0,
                        maxWidth = expandedWidth,
                        minHeight = constraints.minHeight,
                        maxHeight = constraints.maxHeight
                    )
                )
                val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                layout(layoutWidth, placeable.height) {
                    placeable.placeRelative(x = -startOutsetPx, y = 0)
                }
            }
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
                .semantics { contentDescription = "Kembali" }
                .testTag("detail-purchase-back-btn"),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronLeft,
                tint = OnBrand,
                size = 24.dp
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            var maxTitleWidthPx by remember { mutableIntStateOf(0) }
            val titleSize = remember(maxTitleWidthPx, density.fontScale) {
                if (maxTitleWidthPx <= 0) {
                    18.sp
                } else {
                    val candidates = listOf(18f, 17f, 16f, 15f, 14f, 13f)
                    candidates.firstOrNull { sizeSp ->
                        textMeasurer.measure(
                            text = title,
                            style = ownerTextStyle(
                                fontSize = sizeSp.sp,
                                lineHeight = if (sizeSp == 18f) 22.sp else (sizeSp * 1.22f).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBrand,
                                letterSpacing = (-0.01).em
                            ),
                            maxLines = 1,
                            softWrap = false
                        ).size.width <= maxTitleWidthPx
                    }?.sp ?: 13.sp
                }
            }
            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = titleSize,
                    lineHeight = if (titleSize == 18.sp) 22.sp else (titleSize.value * 1.22f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBrand,
                    letterSpacing = (-0.01).em
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { maxTitleWidthPx = it.width }
                    .semantics { heading() }
            )
        }
    }
}

@Composable
private fun PurchaseDetailHeroHeader(
    purchase: PurchaseDetail?,
    matchingPayable: SupplierPayable?,
    isLoading: Boolean,
    onCopyInvoice: (String) -> Unit,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    if (purchase == null) {
        Column(modifier = modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(20.dp))
            Spacer(modifier = Modifier.height(20.dp))
            OwnerHistoryHeroSummary(
                totalLabel = if (isLoading) "Memuat nota pembelian..." else "Total pembelian",
                totalAmount = BigDecimal.ZERO,
                totalTestTag = "detail-purchase-total-amount",
                metrics = listOf(
                    OwnerHistoryHeroMetric("Dibayar", "Rp 0", "detail-purchase-paid"),
                    OwnerHistoryHeroMetric("Sisa utang", "Rp 0", "detail-purchase-debt")
                ),
                isTablet = isTablet
            )
        }
        return
    }

    val breakdown = remember(purchase, matchingPayable) {
        resolvePurchasePaymentBreakdown(purchase, matchingPayable)
    }
    val rawInvoice = purchase.invoiceNo?.trim().orEmpty()
    val hasInvoice = rawInvoice.isNotEmpty()
    val invoiceDisplay = if (hasInvoice) rawInvoice else "Tanpa nomor nota"

    Column(modifier = modifier.fillMaxWidth()) {
        PurchaseDetailStatusLine(
            isUnpaid = breakdown.isUnpaid,
            invoiceDisplay = invoiceDisplay,
            canCopyInvoice = hasInvoice,
            onCopyClick = { if (hasInvoice) onCopyInvoice(rawInvoice) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        OwnerHistoryHeroSummary(
            totalLabel = "Total pembelian",
            totalAmount = breakdown.total,
            totalTestTag = "detail-purchase-total-amount",
            metrics = listOf(
                OwnerHistoryHeroMetric(
                    label = "Dibayar",
                    value = formatSalesColumnCompactRupiah(breakdown.paidAmount),
                    valueTestTag = "detail-purchase-paid"
                ),
                OwnerHistoryHeroMetric(
                    label = "Sisa utang",
                    value = formatSalesColumnCompactRupiah(breakdown.remainingAmount),
                    valueTestTag = "detail-purchase-debt"
                )
            ),
            isTablet = isTablet
        )
    }
}

@Composable
internal fun PurchaseDetailStatusLine(
    isUnpaid: Boolean,
    invoiceDisplay: String,
    canCopyInvoice: Boolean,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = if (isUnpaid) "Hutang" else "Lunas"
    val dotColor = if (isUnpaid) DetailNotaHutangDotColor else DetailNotaLunasDotColor
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var rowWidthPx by remember { mutableIntStateOf(0) }
    val statusAndFixedWidthPx = remember(statusText, canCopyInvoice, density.fontScale) {
        val statusW = textMeasurer.measure(
            text = "$statusText \u00B7 ",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand
            ),
            maxLines = 1,
            softWrap = false
        ).size.width
        val extraPx = with(density) { (8.dp + 16.dp + if (canCopyInvoice) 28.dp else 0.dp).roundToPx() }
        statusW + extraPx
    }
    val availableInvoicePx = (rowWidthPx - statusAndFixedWidthPx).coerceAtLeast(1)
    val invoiceTextSize = remember(invoiceDisplay, rowWidthPx, availableInvoicePx, density.fontScale) {
        if (rowWidthPx <= 0) {
            14.sp
        } else {
            listOf(14f, 13f, 12f, 11f).firstOrNull { sizeSp ->
                textMeasurer.measure(
                    text = invoiceDisplay,
                    style = ownerTextStyle(
                        fontSize = sizeSp.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = BrandSoftText,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    softWrap = false
                ).size.width <= availableInvoicePx
            }?.sp ?: 11.sp
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .onSizeChanged { rowWidthPx = it.width },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
                .testTag("detail-purchase-status-dot")
        )
        Text(
            text = statusText,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand
            ),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .cssLineBoxHeight(20.dp)
                .testTag("detail-purchase-status")
        )
        Text(
            text = "\u00B7",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )
        Text(
            text = invoiceDisplay,
            style = ownerTextStyle(
                fontSize = invoiceTextSize,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText,
                tabularNumbers = true
            ),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .cssLineBoxHeight(20.dp)
                .testTag("detail-purchase-invoice-no")
        )
        if (canCopyInvoice) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center, unbounded = true)
                    .offset(x = (-4).dp)
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onCopyClick
                    )
                    .semantics { contentDescription = "Salin nomor nota" }
                    .testTag("detail-purchase-copy-invoice-btn"),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.Copy,
                    tint = BrandSoftText,
                    size = 18.dp
                )
            }
        }
    }
}

@Composable
private fun DetailNotaSheetContent(
    uiState: OwnerPurchaseDetailUiState,
    onRetry: () -> Unit,
    onSupplierClick: ((String) -> Unit)?
) {
    when {
        uiState.isLoading && uiState.purchase == null -> DetailNotaSkeleton()
        uiState.errorMessage != null && uiState.purchase == null -> DetailNotaErrorBlock(
            message = uiState.errorMessage,
            onRetry = onRetry
        )
        uiState.purchase != null -> DetailNotaBody(
            purchase = uiState.purchase,
            matchingPayable = uiState.matchingPayable,
            onSupplierClick = onSupplierClick
        )
    }
}

@Composable
private fun DetailNotaBody(
    purchase: PurchaseDetail,
    matchingPayable: SupplierPayable?,
    onSupplierClick: ((String) -> Unit)?
) {
    val breakdown = remember(purchase, matchingPayable) {
        resolvePurchasePaymentBreakdown(purchase, matchingPayable)
    }
    val supplierDisplay = purchase.supplierName.trim().ifEmpty { "Tanpa Supplier" }
    val dateDisplay = formatPurchaseDetailDateTime(purchase.receivedAt.ifBlank { purchase.createdAt })
    val notesTrimmed = purchase.notes?.trim()?.takeIf { it.isNotEmpty() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Informasi nota",
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            modifier = Modifier
                .padding(bottom = 4.dp)
                .cssLineBoxHeight(20.dp)
                .semantics { heading() }
        )

        SupplierLinkInfoRow(
            supplierName = supplierDisplay,
            onClick = if (onSupplierClick != null && purchase.supplierId.isNotBlank()) {
                { onSupplierClick(purchase.supplierId) }
            } else {
                null
            }
        )

        DetailHairlineDivider()

        InfoRow(
            label = "Tanggal diterima",
            value = dateDisplay,
            tabularValue = true,
            valueTestTag = "detail-purchase-received-at"
        )

        if (breakdown.isUnpaid && breakdown.dueDate != null) {
            DetailHairlineDivider()
            DueDateInfoRow(
                dueDateDisplay = formatPurchaseDateOnly(breakdown.dueDate),
                isOverdue = breakdown.isDueOverdue
            )
        }

        if (notesTrimmed != null) {
            DetailHairlineDivider()
            MultilineInfoRow(
                label = "Catatan",
                value = notesTrimmed,
                isPlaceholder = false,
                valueTestTag = "detail-purchase-notes"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar barang",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier
                    .cssLineBoxHeight(20.dp)
                    .semantics { heading() }
            )
            Text(
                text = "${purchase.items.size} macam",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted,
                    tabularNumbers = true
                ),
                modifier = Modifier
                    .cssLineBoxHeight(18.dp)
                    .testTag("detail-purchase-item-count")
            )
        }

        if (purchase.items.isEmpty()) {
            Text(
                text = "Tidak ada item barang pada nota ini.",
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            purchase.items.forEachIndexed { index, item ->
                LineItemRow(item = item, index = index)
                if (index < purchase.items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp)
                            .height(1.dp)
                            .background(OwnerDivider)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierLinkInfoRow(
    supplierName: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .let { base ->
                if (onClick != null) {
                    base.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    base
                }
            }
            .testTag("detail-purchase-supplier-row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Supplier",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = supplierName,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Brand
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .cssLineBoxHeight(20.dp)
                    .testTag("detail-purchase-supplier")
            )

            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronRight,
                tint = Brand,
                size = 20.dp
            )
        }
    }
}

@Composable
private fun DueDateInfoRow(
    dueDateDisplay: String,
    isOverdue: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Jatuh tempo",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Text(
            text = dueDateDisplay,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isOverdue) OwnerAmberInk else Ink,
                tabularNumbers = true
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .cssLineBoxHeight(20.dp)
                .testTag("detail-purchase-due-date")
        )
    }
}

@Composable
internal fun LineItemRow(
    item: PurchaseItemDetail,
    index: Int,
    modifier: Modifier = Modifier
) {
    val itemTitle = item.productName.trim().ifEmpty { "Barang #${item.productId.takeLast(4)}" }
    val qtyText = item.quantity.stripTrailingZeros().toPlainString()
    val unitPriceText = formatRupiahWhole(item.priceAtTransaction)
    val subtotalText = formatRupiahWhole(item.subtotal)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(vertical = 16.dp)
            .testTag("detail-purchase-item-$index"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceWhite)
                .border(1.dp, OwnerBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.Box,
                tint = Brand,
                size = 22.dp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = itemTitle,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$qtyText \u00D7 $unitPriceText",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted,
                    tabularNumbers = true
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(18.dp)
            )
        }

        Text(
            text = subtotalText,
            style = ownerTextStyle(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                tabularNumbers = true
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )
    }
}

@Composable
private fun DetailHairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OwnerDivider)
    )
}

@Composable
private fun DetailNotaSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(2) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ChipBg)
                )
                Box(
                    modifier = Modifier
                        .width(136.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ChipBg)
                )
            }
            if (index == 0) DetailHairlineDivider()
        }

        Spacer(modifier = Modifier.height(32.dp))

        repeat(2) { index ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp)
                        .height(1.dp)
                        .background(OwnerDivider)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ChipBg)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipBg)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipBg)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(15.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ChipBg)
                )
            }
        }
    }
}

@Composable
private fun DetailNotaErrorBlock(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Nota pembelian gagal dimuat",
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brand)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onRetry
                )
                .padding(horizontal = 24.dp)
                .testTag("detail-purchase-retry-btn"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Muat ulang",
                style = ownerTextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBrand
                )
            )
        }
    }
}

internal fun formatPurchaseDetailDateTime(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "-"
    runCatching {
        val zdt = OffsetDateTime.parse(trimmed).atZoneSameInstant(PURCHASE_DETAIL_WIB_ZONE)
        val dateText = formatIndonesianDateShort(zdt.toLocalDate())
        val timeText = String.format(Locale.ROOT, "%02d:%02d", zdt.hour, zdt.minute)
        return "$dateText, $timeText"
    }
    runCatching {
        val date = LocalDate.parse(trimmed.take(10))
        return formatIndonesianDateShort(date)
    }
    return trimmed
}

internal fun formatPurchaseDateOnly(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "-"
    val localDate = parsePurchaseDateToLocalDate(trimmed)
    return if (localDate != null) formatIndonesianDateShort(localDate) else trimmed
}

private fun parsePurchaseDateToLocalDate(raw: String): LocalDate? =
    runCatching { OffsetDateTime.parse(raw.trim()).atZoneSameInstant(PURCHASE_DETAIL_WIB_ZONE).toLocalDate() }
        .getOrNull()
        ?: runCatching { LocalDate.parse(raw.trim().take(10)) }.getOrNull()

internal fun sampleReferencePurchaseDetail(): PurchaseDetail = PurchaseDetail(
    id = "p-detail-1",
    supplierId = "s1",
    supplierName = "Supri",
    invoiceNo = "BM-20261123-001",
    total = BigDecimal("380000"),
    notes = null,
    receivedAt = "2026-11-23T11:32:00Z",
    createdAt = "2026-11-23T11:32:00Z",
    items = listOf(
        PurchaseItemDetail(
            productId = "prod-1",
            productName = "Semen Merdeka",
            quantity = BigDecimal("10"),
            priceAtTransaction = BigDecimal("38000"),
            subtotal = BigDecimal("380000")
        )
    )
)

internal fun sampleReferencePurchasePayable(): SupplierPayable = SupplierPayable(
    id = "pay-1",
    supplierId = "s1",
    supplierName = "CV Sinar Baja",
    purchaseId = "p-detail-2",
    amount = BigDecimal("2600000"),
    paidAmount = BigDecimal("600000"),
    remainingAmount = BigDecimal("2000000"),
    dueDate = "2026-11-15T00:00:00Z",
    status = "sebagian",
    createdAt = "2026-11-01T03:15:00Z"
)

@Preview(
    name = "Barokah Jaya Detail Nota Pembelian - Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
internal fun OwnerDetailNotaPembelianPhonePreview() {
    TbterminalappTheme {
        OwnerDetailNotaPembelianScreen(
            uiState = OwnerPurchaseDetailUiState(
                purchase = sampleReferencePurchaseDetail(),
                matchingPayable = null,
                isLoading = false
            ),
            onBack = {},
            onRetry = {},
            onCopyInvoice = {},
            onSupplierClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Detail Nota Pembelian - Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
internal fun OwnerDetailNotaPembelianTabletPreview() {
    TbterminalappTheme {
        OwnerDetailNotaPembelianScreen(
            uiState = OwnerPurchaseDetailUiState(
                purchase = sampleReferencePurchaseDetail(),
                matchingPayable = null,
                isLoading = false
            ),
            onBack = {},
            onRetry = {},
            onCopyInvoice = {},
            onSupplierClick = {},
            onNavSelect = {}
        )
    }
}
