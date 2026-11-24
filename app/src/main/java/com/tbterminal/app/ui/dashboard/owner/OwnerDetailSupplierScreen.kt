package com.tbterminal.app.ui.dashboard.owner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.TbTerminalApplication
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.suppliers.SupplierDetailUiState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val OWNER_DETAIL_SUPPLIER_TABLET_BREAKPOINT = 840.dp
private val SupplierDangerColor = Color(0xFFB3123F)
private val SupplierDangerBorderColor = Color(0xFFF5C6D3)
private val SupplierActiveDotColor = Color(0xFF1F9D63)

internal const val SUPPLIER_REACTIVATE_ENABLED: Boolean = true

internal object OwnerSupplierToastBridge {
    private var pendingMessage: String? = null

    fun setPendingSuccess(message: String) {
        pendingMessage = message
    }

    fun consumePendingSuccess(): String? {
        val value = pendingMessage
        pendingMessage = null
        return value
    }
}

private enum class DetailSupplierMutationAction {
    DEACTIVATE,
    ACTIVATE,
    UNDO_ACTIVATE
}

@Composable
internal fun OwnerDetailSupplierRoute(
    uiState: SupplierDetailUiState,
    canManage: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditSupplierClick: (String) -> Unit,
    onDeactivateConfirm: () -> Unit,
    onClearActionError: () -> Unit,
    onDeactivatedReset: () -> Unit,
    onDashboardClick: () -> Unit,
    onActivateClick: () -> Unit = {},
    onUndoActivateClick: () -> Unit = {},
    onReactivatedReset: () -> Unit = {},
    reactivateEnabled: Boolean = SUPPLIER_REACTIVATE_ENABLED,
    isOnlineOverride: Boolean? = null
) {
    val context = LocalContext.current
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val appContainer = (context.applicationContext as? TbTerminalApplication)?.appContainer
    val offlineStatusFlow = remember(appContainer) { appContainer?.offlineStatusRepository?.status }
    val offlineStatus by (
        offlineStatusFlow?.collectAsState(initial = OfflineStatus())
            ?: remember { mutableStateOf(OfflineStatus()) }
        )
    val isOnline = isOnlineOverride ?: (
        offlineStatus.isInternetOnline &&
            offlineStatus.backendStatus != BackendStatus.UNREACHABLE
        )

    var showDeactivateConfirm by remember { mutableStateOf(false) }
    var copiedPhoneValue by remember { mutableStateOf<String?>(null) }
    var reactivatedToastName by remember { mutableStateOf<String?>(null) }
    var savedToastMessage by remember { mutableStateOf<String?>(null) }
    var lastAction by remember { mutableStateOf<DetailSupplierMutationAction?>(null) }

    BackHandler(enabled = !uiState.isMutating, onBack = onBack)

    LaunchedEffect(uiState.supplier?.updatedAt, uiState.isLoading) {
        if (!uiState.isLoading && uiState.supplier != null) {
            OwnerSupplierToastBridge.consumePendingSuccess()?.let {
                savedToastMessage = it
            }
        }
    }

    LaunchedEffect(uiState.isDeactivated) {
        if (uiState.isDeactivated) {
            showDeactivateConfirm = false
            OwnerSupplierToastBridge.setPendingSuccess("Supplier dinonaktifkan")
            onDeactivatedReset()
            onBack()
        }
    }

    LaunchedEffect(uiState.isReactivated) {
        if (uiState.isReactivated) {
            reactivatedToastName = uiState.supplier?.name ?: "Supplier"
            onReactivatedReset()
        }
    }

    LaunchedEffect(uiState.actionErrorMessage) {
        if (uiState.actionErrorMessage != null) {
            showDeactivateConfirm = false
        }
    }

    fun handleCopyPhone(rawPhone: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Nomor HP", rawPhone))
        copiedPhoneValue = rawPhone
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        when (destination) {
            DashboardNavDestination.BERANDA ->
                destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
            DashboardNavDestination.TRANSAKSI ->
                destinationNavigator?.invoke(AdminDestination.TransactionsHub)
            DashboardNavDestination.KEUANGAN ->
                destinationNavigator?.invoke(AdminDestination.FinanceHub)
            DashboardNavDestination.STOK ->
                destinationNavigator?.invoke(AdminDestination.StockHub)
            DashboardNavDestination.MENU -> onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OwnerDetailSupplierScreen(
            uiState = uiState,
            canManage = canManage,
            reactivateEnabled = reactivateEnabled,
            isOnline = isOnline,
            onBack = onBack,
            onRetry = onRetry,
            onEditClick = { uiState.supplier?.let { onEditSupplierClick(it.id) } },
            onCopyPhone = ::handleCopyPhone,
            onDeactivateClick = { showDeactivateConfirm = true },
            onActivateClick = {
                reactivatedToastName = null
                lastAction = DetailSupplierMutationAction.ACTIVATE
                onActivateClick()
            },
            onNavSelect = ::handleNavSelect
        )

        AppAlert(
            type = AlertType.Success,
            title = "Nomor HP disalin",
            message = copiedPhoneValue.orEmpty(),
            visible = copiedPhoneValue != null,
            onDismiss = { copiedPhoneValue = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("detail-supplier-copy-toast")
        )

        AppAlert(
            type = AlertType.Success,
            title = "Supplier diaktifkan kembali",
            message = reactivatedToastName?.let { "$it kini berstatus aktif." }.orEmpty(),
            actionLabel = if (canManage && isOnline) "Batalkan" else null,
            onAction = {
                reactivatedToastName = null
                lastAction = DetailSupplierMutationAction.UNDO_ACTIVATE
                onUndoActivateClick()
            },
            visible = copiedPhoneValue == null && uiState.actionErrorMessage == null && reactivatedToastName != null,
            onDismiss = { reactivatedToastName = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("detail-supplier-reactivated-toast")
        )

        AppAlert(
            type = AlertType.Success,
            title = savedToastMessage.orEmpty(),
            message = "",
            visible = copiedPhoneValue == null &&
                uiState.actionErrorMessage == null &&
                reactivatedToastName == null &&
                !savedToastMessage.isNullOrBlank(),
            onDismiss = { savedToastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("detail-supplier-saved-toast")
        )

        val actionError = uiState.actionErrorMessage
        if (copiedPhoneValue == null && actionError != null) {
            val isNetwork = listOf("koneksi", "jaringan", "terhubung", "timeout", "network", "offline")
                .any { it in actionError.lowercase() }
            val errorTitle = when {
                uiState.isNotFoundError -> "Supplier tidak ditemukan"
                isNetwork -> "Tidak dapat terhubung"
                lastAction == DetailSupplierMutationAction.ACTIVATE -> "Gagal mengaktifkan"
                else -> "Gagal menonaktifkan"
            }
            AppAlert(
                type = AlertType.Error,
                title = errorTitle,
                message = actionError,
                actionLabel = when {
                    uiState.isNotFoundError -> "Kembali"
                    isNetwork -> "Coba lagi"
                    else -> null
                },
                onAction = {
                    onClearActionError()
                    if (uiState.isNotFoundError) {
                        onBack()
                    } else {
                        when (lastAction) {
                            DetailSupplierMutationAction.ACTIVATE -> onActivateClick()
                            DetailSupplierMutationAction.UNDO_ACTIVATE -> onUndoActivateClick()
                            DetailSupplierMutationAction.DEACTIVATE, null -> onDeactivateConfirm()
                        }
                    }
                },
                visible = true,
                onDismiss = onClearActionError,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .testTag("detail-supplier-action-error-toast")
            )
        }
    }

    if (showDeactivateConfirm && uiState.supplier != null) {
        DeactivateSupplierConfirmDialog(
            supplierName = uiState.supplier.name,
            isBusy = uiState.isMutating,
            onDismiss = { if (!uiState.isMutating) showDeactivateConfirm = false },
            onConfirm = {
                lastAction = DetailSupplierMutationAction.DEACTIVATE
                onDeactivateConfirm()
            }
        )
    }
}

@Composable
internal fun OwnerDetailSupplierScreen(
    uiState: SupplierDetailUiState,
    canManage: Boolean,
    reactivateEnabled: Boolean = SUPPLIER_REACTIVATE_ENABLED,
    isOnline: Boolean = true,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditClick: () -> Unit,
    onCopyPhone: (String) -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit = {},
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_DETAIL_SUPPLIER_TABLET_BREAKPOINT
        if (isTablet) {
            TabletDetailSupplierLayout(
                uiState = uiState,
                canManage = canManage,
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                onBack = onBack,
                onRetry = onRetry,
                onEditClick = onEditClick,
                onCopyPhone = onCopyPhone,
                onDeactivateClick = onDeactivateClick,
                onActivateClick = onActivateClick,
                onNavSelect = onNavSelect
            )
        } else {
            PhoneDetailSupplierLayout(
                uiState = uiState,
                canManage = canManage,
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                onBack = onBack,
                onRetry = onRetry,
                onEditClick = onEditClick,
                onCopyPhone = onCopyPhone,
                onDeactivateClick = onDeactivateClick,
                onActivateClick = onActivateClick
            )
        }
    }
}

@Composable
private fun PhoneDetailSupplierLayout(
    uiState: SupplierDetailUiState,
    canManage: Boolean,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditClick: () -> Unit,
    onCopyPhone: (String) -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .verticalScroll(rememberScrollState())
    ) {
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
                SupplierDetailTopBar(
                    canEdit = canManage && uiState.supplier != null,
                    onBack = onBack,
                    onEditClick = onEditClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                SupplierIdentityHeader(
                    supplier = uiState.supplier,
                    isLoading = uiState.isLoading
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val overlapPx = 28.dp.roundToPx()
                    layout(placeable.width, (placeable.height - overlapPx).coerceAtLeast(0)) {
                        placeable.placeRelative(0, -overlapPx)
                    }
                }
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(SurfaceWhite)
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp)
        ) {
            DetailSupplierSheetContent(
                uiState = uiState,
                canManage = canManage,
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                onRetry = onRetry,
                onCopyPhone = onCopyPhone,
                onDeactivateClick = onDeactivateClick,
                onActivateClick = onActivateClick
            )
        }
    }
}

@Composable
private fun TabletDetailSupplierLayout(
    uiState: SupplierDetailUiState,
    canManage: Boolean,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditClick: () -> Unit,
    onCopyPhone: (String) -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
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
                    SupplierDetailTopBar(
                        canEdit = canManage && uiState.supplier != null,
                        onBack = onBack,
                        onEditClick = onEditClick
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        SupplierIdentityHeader(
                            supplier = uiState.supplier,
                            isLoading = uiState.isLoading
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
                DetailSupplierSheetContent(
                    uiState = uiState,
                    canManage = canManage,
                    reactivateEnabled = reactivateEnabled,
                    isOnline = isOnline,
                    onRetry = onRetry,
                    onCopyPhone = onCopyPhone,
                    onDeactivateClick = onDeactivateClick,
                    onActivateClick = onActivateClick
                )
            }
        }
    }
}

@Composable
private fun SupplierDetailTopBar(
    canEdit: Boolean,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(width = 36.dp, height = 48.dp)) {
            Box(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.CenterEnd, unbounded = true)
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onBack
                    )
                    .semantics { contentDescription = "Kembali" }
                    .testTag("detail-supplier-back-btn"),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.ChevronLeft,
                    tint = OnBrand,
                    size = 24.dp
                )
            }
        }

        Text(
            text = "Detail supplier",
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand,
                letterSpacing = (-0.01).em
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .cssLineBoxHeight(22.dp)
                .semantics { heading() }
        )

        if (canEdit) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onEditClick
                    )
                    .semantics { contentDescription = "Edit supplier" }
                    .testTag("detail-supplier-edit-btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OwnerVectorIcon(
                        imageVector = OwnerIcons.EditPencil,
                        tint = OnBrand,
                        size = 15.dp
                    )
                    Text(
                        text = "Edit",
                        style = ownerTextStyle(
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand
                        ),
                        maxLines = 1,
                        modifier = Modifier.cssLineBoxHeight(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierIdentityHeader(
    supplier: Supplier?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (supplier == null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.Truck,
                    tint = OnBrand,
                    size = 28.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoading) "Memuat supplier..." else "Data tidak tersedia",
                    style = ownerTextStyle(
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBrand
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isLoading) "Mengambil info kontak & termin" else "Silakan muat ulang halaman",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = BrandSoftText
                    )
                )
            }
        }
        return
    }

    val initials = formatProfileTwoLetterInitials(supplier.name)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = ownerTextStyle(
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBrand
                    ),
                    modifier = Modifier.cssLineBoxHeight(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (supplier.isActive) SupplierActiveDotColor else Color(0xFFE11D48))
                    .border(width = 2.dp, color = Brand, shape = CircleShape)
                    .testTag("detail-supplier-status-dot")
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = supplier.name,
                style = ownerTextStyle(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBrand,
                    letterSpacing = (-0.01).em
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 26.dp)
                    .testTag("detail-supplier-name")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp)
                        .testTag("detail-supplier-type-badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Supplier",
                        style = ownerTextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand
                        ),
                        maxLines = 1,
                        modifier = Modifier.cssLineBoxHeight(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSupplierSheetContent(
    uiState: SupplierDetailUiState,
    canManage: Boolean,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    onRetry: () -> Unit,
    onCopyPhone: (String) -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    when {
        uiState.isLoading && uiState.supplier == null -> DetailSupplierSkeleton()
        uiState.errorMessage != null && uiState.supplier == null -> DetailSupplierErrorBlock(
            message = uiState.errorMessage,
            onRetry = onRetry
        )
        uiState.supplier != null -> DetailSupplierBody(
            supplier = uiState.supplier,
            canManage = canManage,
            reactivateEnabled = reactivateEnabled,
            isOnline = isOnline,
            isMutating = uiState.isMutating,
            onCopyPhone = onCopyPhone,
            onDeactivateClick = onDeactivateClick,
            onActivateClick = onActivateClick
        )
    }
}

@Composable
private fun DetailSupplierBody(
    supplier: Supplier,
    canManage: Boolean,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    isMutating: Boolean,
    onCopyPhone: (String) -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    val rawPhone = supplier.phone?.trim().orEmpty()
    val hasPhone = rawPhone.isNotEmpty()
    val phoneDisplay = if (hasPhone) formatCustomerPhoneDisplay(rawPhone) else "Belum diisi"
    val addressDisplay = supplier.address?.trim()?.takeIf { it.isNotEmpty() } ?: "Belum diisi"
    val termDisplay = if (supplier.paymentTermDays > 0) {
        "${supplier.paymentTermDays} hari"
    } else {
        "0 hari (tunai)"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Kontak & alamat",
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            modifier = Modifier
                .padding(bottom = 12.dp)
                .cssLineBoxHeight(20.dp)
                .semantics { heading() }
        )

        InfoRow(
            label = "Nomor HP",
            value = phoneDisplay,
            tabularValue = true,
            onCopyClick = if (hasPhone) {
                { onCopyPhone(rawPhone) }
            } else {
                null
            },
            copyContentDescription = "Salin nomor HP",
            copyTestTag = "detail-supplier-copy-phone-btn",
            valueTestTag = "detail-supplier-phone"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )

        MultilineInfoRow(
            label = "Alamat",
            value = addressDisplay,
            isPlaceholder = supplier.address.isNullOrBlank(),
            valueTestTag = "detail-supplier-address"
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Ketentuan pembayaran",
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            modifier = Modifier
                .padding(bottom = 12.dp)
                .cssLineBoxHeight(20.dp)
                .semantics { heading() }
        )

        InfoRow(
            label = "Termin pembayaran",
            value = termDisplay,
            tabularValue = true,
            valueTestTag = "detail-supplier-term"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )

        InfoRow(
            label = "Status",
            value = if (supplier.isActive) "Aktif" else "Nonaktif",
            tabularValue = false,
            valueTestTag = "detail-supplier-status"
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!supplier.isActive) {
            InactiveSupplierBanner(
                canManage = canManage,
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                isMutating = isMutating,
                onActivateClick = onActivateClick
            )
        } else if (canManage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SupplierDangerBorderColor, RoundedCornerShape(16.dp))
                    .clickable(
                        enabled = !isMutating,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onDeactivateClick
                    )
                    .testTag("detail-supplier-deactivate-btn"),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.UserSlash,
                    tint = SupplierDangerColor,
                    size = 20.dp
                )
                Text(
                    text = if (isMutating) "Menonaktifkan..." else "Nonaktifkan supplier",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SupplierDangerColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InactiveSupplierBanner(
    canManage: Boolean,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    isMutating: Boolean,
    onActivateClick: () -> Unit
) {
    val showActivateAction = reactivateEnabled && canManage
    val message = when {
        showActivateAction && !isOnline -> "Butuh koneksi internet untuk mengaktifkan supplier."
        showActivateAction -> "Supplier nonaktif dan tidak tampil pada pembelian."
        else -> "Supplier ini berstatus nonaktif dan tidak tampil pada pilihan pembelian."
    }
    val actionEnabled = isOnline && !isMutating

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ChipBg)
            .border(1.dp, OwnerBorder, RoundedCornerShape(16.dp))
            .padding(
                start = 16.dp,
                top = 12.dp,
                end = if (showActivateAction) 10.dp else 16.dp,
                bottom = 12.dp
            )
            .testTag("detail-supplier-inactive-banner"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.AlertCircle,
            tint = if (showActivateAction && !isOnline) SupplierDangerColor else Muted,
            size = 20.dp
        )
        Text(
            text = message,
            style = ownerTextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = if (showActivateAction && !isOnline) Ink else Muted
            ),
            modifier = Modifier.weight(1f)
        )
        if (showActivateAction) {
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        enabled = actionEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onActivateClick
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("detail-supplier-activate-btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMutating) "Memproses..." else "Aktifkan",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (actionEnabled) Brand else Muted
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DetailSupplierSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detail-supplier-loading-skeleton"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBox(modifier = Modifier.width(130.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.width(88.dp).height(14.dp), shape = RoundedCornerShape(6.dp))
                SkeletonBox(modifier = Modifier.width(150.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(modifier = Modifier.width(130.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.width(110.dp).height(14.dp), shape = RoundedCornerShape(6.dp))
                SkeletonBox(modifier = Modifier.width(120.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp))
    }
}

@Composable
private fun DetailSupplierErrorBlock(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag("detail-supplier-error-block"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ChipBg),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ErrorCircle,
                tint = SupplierDangerColor,
                size = 28.dp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Gagal memuat detail supplier",
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
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
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brand)
                .clickable(role = Role.Button, onClick = onRetry)
                .padding(horizontal = 24.dp)
                .testTag("detail-supplier-retry-btn"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Coba lagi",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBrand
                )
            )
        }
    }
}

@Composable
internal fun DeactivateSupplierConfirmDialog(
    supplierName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp)
                .testTag("detail-supplier-deactivate-dialog")
        ) {
            Text(
                text = "Nonaktifkan supplier?",
                style = ownerTextStyle(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$supplierName tidak akan lagi tampil pada daftar aktif atau pilihan pembelian.",
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = !isBusy,
                            role = Role.Button,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp)
                        .testTag("detail-supplier-deactivate-cancel"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Batal",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brand
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SupplierDangerColor)
                        .clickable(
                            enabled = !isBusy,
                            role = Role.Button,
                            onClick = onConfirm
                        )
                        .padding(horizontal = 20.dp)
                        .testTag("detail-supplier-deactivate-confirm"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBusy) "Memproses..." else "Nonaktifkan",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand
                        )
                    )
                }
            }
        }
    }
}

internal fun sampleDetailSupplier(isActive: Boolean = true): Supplier = Supplier(
    id = "sup-01",
    name = "PT Semen Merdeka",
    phone = "081234567890",
    address = "Jl. Raya Industri No. 12, Jakarta Timur",
    paymentTermDays = 30,
    isActive = isActive,
    createdAt = "2026-01-10T08:00:00Z",
    updatedAt = "2026-02-15T10:30:00Z"
)

@Preview(
    name = "OwnerDetailSupplierPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 880
)
@Composable
internal fun OwnerDetailSupplierPhonePreview() {
    TbterminalappTheme {
        OwnerDetailSupplierScreen(
            uiState = SupplierDetailUiState(
                supplier = sampleDetailSupplier(isActive = true),
                isLoading = false
            ),
            canManage = true,
            onBack = {},
            onRetry = {},
            onEditClick = {},
            onCopyPhone = {},
            onDeactivateClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerDetailSupplierTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerDetailSupplierTabletPreview() {
    TbterminalappTheme {
        OwnerDetailSupplierScreen(
            uiState = SupplierDetailUiState(
                supplier = sampleDetailSupplier(isActive = true),
                isLoading = false
            ),
            canManage = true,
            onBack = {},
            onRetry = {},
            onEditClick = {},
            onCopyPhone = {},
            onDeactivateClick = {},
            onNavSelect = {}
        )
    }
}
