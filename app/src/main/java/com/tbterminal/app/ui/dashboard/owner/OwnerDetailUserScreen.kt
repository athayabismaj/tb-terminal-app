package com.tbterminal.app.ui.dashboard.owner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.settings.CredentialChangeDialog
import com.tbterminal.app.ui.settings.CredentialDialog
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
import com.tbterminal.app.ui.users.StaffMember
import com.tbterminal.app.ui.users.StaffRole
import com.tbterminal.app.ui.users.StaffStatus
import com.tbterminal.app.ui.users.UserDetailUiState

private val OWNER_DETAIL_USER_TABLET_BREAKPOINT = 840.dp
private val UserDangerColor = Color(0xFFB3123F)
private val UserDangerBorderColor = Color(0xFFF5C6D3)
private val UserActiveDotColor = Color(0xFF1F9D63)

internal const val USER_REACTIVATE_ENABLED: Boolean = true

private enum class DetailUserMutationAction {
    DEACTIVATE,
    ACTIVATE,
    UNDO_ACTIVATE
}

private fun StaffRole.detailBadgeLabel(): String {
    return when (this) {
        StaffRole.Owner -> "Owner"
        StaffRole.Admin -> "Admin"
        StaffRole.Kasir -> "Kasir"
        StaffRole.Other -> "Staf"
    }
}

@Composable
internal fun OwnerDetailUserRoute(
    uiState: UserDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditUserClick: (String) -> Unit,
    onChangePassword: (newPassword: String, confirmPassword: String) -> Unit = { _, _ -> },
    onChangePin: (newPin: String, confirmPin: String) -> Unit = { _, _ -> },
    onClearCredentialMessages: () -> Unit = {},
    onDeactivateConfirm: () -> Unit,
    onClearActionError: () -> Unit,
    onDeactivatedReset: () -> Unit,
    onDashboardClick: () -> Unit,
    onActivateClick: () -> Unit = {},
    onUndoActivateClick: () -> Unit = {},
    onReactivatedReset: () -> Unit = {},
    reactivateEnabled: Boolean = USER_REACTIVATE_ENABLED,
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
    var credentialDialog by remember { mutableStateOf<CredentialDialog?>(null) }
    var copiedUsernameValue by remember { mutableStateOf<String?>(null) }
    var reactivatedToastName by remember { mutableStateOf<String?>(null) }
    var savedToastMessage by remember { mutableStateOf<String?>(null) }
    var lastAction by remember { mutableStateOf<DetailUserMutationAction?>(null) }

    BackHandler(enabled = !uiState.isMutating, onBack = onBack)

    LaunchedEffect(uiState.staff, uiState.isLoading) {
        if (!uiState.isLoading && uiState.staff != null) {
            OwnerUserToastBridge.consumePendingSuccess()?.let {
                savedToastMessage = it
            }
        }
    }

    LaunchedEffect(uiState.credentialSuccessMessage) {
        val successMessage = uiState.credentialSuccessMessage
        if (successMessage != null) {
            credentialDialog = null
            copiedUsernameValue = null
            reactivatedToastName = null
            savedToastMessage = successMessage
            onClearCredentialMessages()
        }
    }

    LaunchedEffect(uiState.isDeactivated) {
        if (uiState.isDeactivated) {
            showDeactivateConfirm = false
            OwnerUserToastBridge.setPendingSuccess("Pengguna dinonaktifkan")
            onDeactivatedReset()
            onBack()
        }
    }

    LaunchedEffect(uiState.isReactivated) {
        if (uiState.isReactivated) {
            reactivatedToastName = uiState.staff?.name ?: "Pengguna"
            onReactivatedReset()
        }
    }

    LaunchedEffect(uiState.actionErrorMessage) {
        if (uiState.actionErrorMessage != null) {
            showDeactivateConfirm = false
        }
    }

    fun handleCopyUsername(rawUsername: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Username", rawUsername))
        copiedUsernameValue = "@$rawUsername"
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
        OwnerDetailUserScreen(
            uiState = uiState,
            reactivateEnabled = reactivateEnabled,
            isOnline = isOnline,
            onBack = onBack,
            onRetry = onRetry,
            onEditClick = { uiState.staff?.let { onEditUserClick(it.id) } },
            onChangePasswordClick = {
                copiedUsernameValue = null
                savedToastMessage = null
                onClearCredentialMessages()
                credentialDialog = CredentialDialog.PASSWORD
            },
            onChangePinClick = {
                copiedUsernameValue = null
                savedToastMessage = null
                onClearCredentialMessages()
                credentialDialog = CredentialDialog.PIN
            },
            onCopyUsername = ::handleCopyUsername,
            onDeactivateClick = {
                copiedUsernameValue = null
                showDeactivateConfirm = true
            },
            onActivateClick = {
                reactivatedToastName = null
                lastAction = DetailUserMutationAction.ACTIVATE
                onActivateClick()
            },
            onNavSelect = ::handleNavSelect
        )

        AppAlert(
            type = AlertType.Success,
            title = "Username disalin",
            message = copiedUsernameValue.orEmpty(),
            visible = copiedUsernameValue != null,
            onDismiss = { copiedUsernameValue = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("detail-user-copy-toast")
        )

        AppAlert(
            type = AlertType.Success,
            title = "Pengguna diaktifkan kembali",
            message = reactivatedToastName?.let { "$it kini berstatus aktif." }.orEmpty(),
            actionLabel = if (isOnline) "Batalkan" else null,
            onAction = {
                reactivatedToastName = null
                lastAction = DetailUserMutationAction.UNDO_ACTIVATE
                onUndoActivateClick()
            },
            visible = copiedUsernameValue == null && uiState.actionErrorMessage == null && reactivatedToastName != null,
            onDismiss = { reactivatedToastName = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("detail-user-reactivated-toast")
        )

        AppAlert(
            type = AlertType.Success,
            title = savedToastMessage.orEmpty(),
            message = "",
            visible = copiedUsernameValue == null &&
                uiState.actionErrorMessage == null &&
                reactivatedToastName == null &&
                !savedToastMessage.isNullOrBlank(),
            onDismiss = { savedToastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("detail-user-saved-toast")
        )

        val actionError = uiState.actionErrorMessage
        if (copiedUsernameValue == null && actionError != null) {
            val isNetwork = listOf("koneksi", "jaringan", "terhubung", "timeout", "network", "offline")
                .any { it in actionError.lowercase() }
            val errorTitle = when {
                uiState.isNotFoundError -> "Pengguna tidak ditemukan"
                isNetwork -> "Tidak dapat terhubung"
                lastAction == DetailUserMutationAction.ACTIVATE -> "Gagal mengaktifkan"
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
                            DetailUserMutationAction.ACTIVATE -> onActivateClick()
                            DetailUserMutationAction.UNDO_ACTIVATE -> onUndoActivateClick()
                            DetailUserMutationAction.DEACTIVATE, null -> onDeactivateConfirm()
                        }
                    }
                },
                visible = true,
                onDismiss = onClearActionError,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .testTag("detail-user-action-error-toast")
            )
        }
    }

    if (showDeactivateConfirm && uiState.staff != null) {
        DeactivateUserConfirmDialog(
            userName = uiState.staff.name,
            isBusy = uiState.isMutating,
            onDismiss = { if (!uiState.isMutating) showDeactivateConfirm = false },
            onConfirm = {
                lastAction = DetailUserMutationAction.DEACTIVATE
                onDeactivateConfirm()
            }
        )
    }

    credentialDialog?.let { type ->
        CredentialChangeDialog(
            type = type,
            isSaving = uiState.isMutating,
            errorMessage = uiState.credentialErrorMessage,
            onInputChanged = onClearCredentialMessages,
            onDismiss = {
                if (!uiState.isMutating) {
                    credentialDialog = null
                    onClearCredentialMessages()
                }
            },
            onSubmit = { _, newVal, confirmVal ->
                if (type == CredentialDialog.PASSWORD) {
                    onChangePassword(newVal, confirmVal)
                } else {
                    onChangePin(newVal, confirmVal)
                }
            },
            requireCurrentCredential = false
        )
    }
}

@Composable
internal fun OwnerDetailUserScreen(
    uiState: UserDetailUiState,
    reactivateEnabled: Boolean = USER_REACTIVATE_ENABLED,
    isOnline: Boolean = true,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onCopyUsername: (String) -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit = {},
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_DETAIL_USER_TABLET_BREAKPOINT
        if (isTablet) {
            TabletDetailUserLayout(
                uiState = uiState,
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                onBack = onBack,
                onRetry = onRetry,
                onEditClick = onEditClick,
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick,
                onCopyUsername = onCopyUsername,
                onDeactivateClick = onDeactivateClick,
                onActivateClick = onActivateClick,
                onNavSelect = onNavSelect
            )
        } else {
            PhoneDetailUserLayout(
                uiState = uiState,
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                onBack = onBack,
                onRetry = onRetry,
                onEditClick = onEditClick,
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick,
                onCopyUsername = onCopyUsername,
                onDeactivateClick = onDeactivateClick,
                onActivateClick = onActivateClick
            )
        }
    }
}

@Composable
private fun PhoneDetailUserLayout(
    uiState: UserDetailUiState,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onCopyUsername: (String) -> Unit,
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
                UserDetailTopBar(
                    canEdit = uiState.staff != null,
                    onBack = onBack,
                    onEditClick = onEditClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                UserIdentityHeader(
                    staff = uiState.staff,
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
            DetailUserSheetContent(
                uiState = uiState,
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                onRetry = onRetry,
                onCopyUsername = onCopyUsername,
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick,
                onDeactivateClick = onDeactivateClick,
                onActivateClick = onActivateClick
            )
        }
    }
}

@Composable
private fun TabletDetailUserLayout(
    uiState: UserDetailUiState,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onCopyUsername: (String) -> Unit,
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
                    UserDetailTopBar(
                        canEdit = uiState.staff != null,
                        onBack = onBack,
                        onEditClick = onEditClick
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        UserIdentityHeader(
                            staff = uiState.staff,
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
                DetailUserSheetContent(
                    uiState = uiState,
                    reactivateEnabled = reactivateEnabled,
                    isOnline = isOnline,
                    onRetry = onRetry,
                    onCopyUsername = onCopyUsername,
                    onChangePasswordClick = onChangePasswordClick,
                    onChangePinClick = onChangePinClick,
                    onDeactivateClick = onDeactivateClick,
                    onActivateClick = onActivateClick
                )
            }
        }
    }
}

@Composable
private fun UserDetailTopBar(
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
                    .testTag("detail-user-back-btn"),
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
            text = "Detail pengguna",
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
                    .semantics { contentDescription = "Edit pengguna" }
                    .testTag("detail-user-edit-btn"),
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
private fun UserIdentityHeader(
    staff: StaffMember?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (staff == null) {
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
                    imageVector = OwnerIcons.UserSingle,
                    tint = OnBrand,
                    size = 28.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoading) "Memuat pengguna..." else "Data tidak tersedia",
                    style = ownerTextStyle(
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBrand
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isLoading) "Mengambil info akun & akses" else "Silakan muat ulang halaman",
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

    val isActive = staff.status == StaffStatus.Active
    val initials = formatProfileTwoLetterInitials(staff.name)
    val roleLabel = staff.role.detailBadgeLabel()

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
                    .background(if (isActive) UserActiveDotColor else Color(0xFFE11D48))
                    .border(width = 2.dp, color = Brand, shape = CircleShape)
                    .testTag("detail-user-status-dot")
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = staff.name,
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
                    .testTag("detail-user-name")
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
                        .testTag("detail-user-role-badge"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = roleLabel,
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
private fun DetailUserSheetContent(
    uiState: UserDetailUiState,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    onRetry: () -> Unit,
    onCopyUsername: (String) -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    when {
        uiState.isLoading && uiState.staff == null -> DetailUserSkeleton()
        uiState.errorMessage != null && uiState.staff == null -> DetailUserErrorBlock(
            message = uiState.errorMessage,
            onRetry = onRetry
        )
        uiState.staff != null -> DetailUserBody(
            staff = uiState.staff,
            reactivateEnabled = reactivateEnabled,
            isOnline = isOnline,
            isMutating = uiState.isMutating,
            onCopyUsername = onCopyUsername,
            onChangePasswordClick = onChangePasswordClick,
            onChangePinClick = onChangePinClick,
            onDeactivateClick = onDeactivateClick,
            onActivateClick = onActivateClick
        )
    }
}

@Composable
private fun DetailUserBody(
    staff: StaffMember,
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    isMutating: Boolean,
    onCopyUsername: (String) -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    val isActive = staff.status == StaffStatus.Active
    val emailDisplay = staff.rawEmail?.trim()?.takeIf { it.isNotEmpty() } ?: "Belum diisi"

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Informasi akun",
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
            label = "Username",
            value = "@${staff.username}",
            tabularValue = false,
            onCopyClick = { onCopyUsername(staff.username) },
            copyContentDescription = "Salin username",
            copyTestTag = "detail-user-copy-username-btn",
            valueTestTag = "detail-user-username"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )

        InfoRow(
            label = "Email",
            value = emailDisplay,
            tabularValue = false,
            valueTestTag = "detail-user-email"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )

        InfoRow(
            label = "Peran",
            value = staff.role.detailBadgeLabel(),
            tabularValue = false,
            valueTestTag = "detail-user-role"
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Aktivitas & status",
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
            label = "Login terakhir",
            value = staff.lastLogin,
            tabularValue = true,
            valueTestTag = "detail-user-last-login"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )

        InfoRow(
            label = "Status",
            value = if (isActive) "Aktif" else "Nonaktif",
            tabularValue = false,
            valueTestTag = "detail-user-status"
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isActive) {
            InactiveUserBanner(
                reactivateEnabled = reactivateEnabled,
                isOnline = isOnline,
                isMutating = isMutating,
                onActivateClick = onActivateClick
            )
        } else {
            val securityGroup = remember(onChangePasswordClick, onChangePinClick) {
                OwnerPlainMenuGroup(
                    title = "Keamanan",
                    items = listOf(
                        OwnerPlainMenuItem(
                            tag = "detail-user-password-btn",
                            title = "Ubah kata sandi",
                            subtitle = null,
                            icon = OwnerIcons.LockClosed,
                            iconTint = Brand,
                            onClick = onChangePasswordClick
                        ),
                        OwnerPlainMenuItem(
                            tag = "detail-user-pin-btn",
                            title = "Ubah PIN transaksi",
                            subtitle = "PIN 6 digit untuk konfirmasi",
                            icon = OwnerIcons.PinDots,
                            iconTint = Brand,
                            onClick = onChangePinClick
                        )
                    )
                )
            }

            OwnerPlainMenuSection(group = securityGroup)

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, UserDangerBorderColor, RoundedCornerShape(16.dp))
                    .clickable(
                        enabled = !isMutating,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onDeactivateClick
                    )
                    .testTag("detail-user-deactivate-btn"),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.UserSlash,
                    tint = UserDangerColor,
                    size = 20.dp
                )
                Text(
                    text = if (isMutating) "Menonaktifkan..." else "Nonaktifkan pengguna",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UserDangerColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InactiveUserBanner(
    reactivateEnabled: Boolean,
    isOnline: Boolean,
    isMutating: Boolean,
    onActivateClick: () -> Unit
) {
    val message = when {
        reactivateEnabled && !isOnline -> "Butuh koneksi internet untuk mengaktifkan pengguna."
        reactivateEnabled -> "Pengguna nonaktif dan tidak dapat login ke aplikasi."
        else -> "Pengguna ini berstatus nonaktif dan tidak dapat digunakan untuk login."
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
                end = if (reactivateEnabled) 10.dp else 16.dp,
                bottom = 12.dp
            )
            .testTag("detail-user-inactive-banner"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.AlertCircle,
            tint = if (reactivateEnabled && !isOnline) UserDangerColor else Muted,
            size = 20.dp
        )
        Text(
            text = message,
            style = ownerTextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = if (reactivateEnabled && !isOnline) Ink else Muted
            ),
            modifier = Modifier.weight(1f)
        )
        if (reactivateEnabled) {
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
                    .testTag("detail-user-activate-btn"),
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
private fun DetailUserSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detail-user-loading-skeleton"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBox(modifier = Modifier.width(130.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
        repeat(3) {
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
private fun DetailUserErrorBlock(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag("detail-user-error-block"),
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
                tint = UserDangerColor,
                size = 28.dp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Gagal memuat detail pengguna",
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
                .testTag("detail-user-retry-btn"),
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
internal fun DeactivateUserConfirmDialog(
    userName: String,
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
                .testTag("detail-user-deactivate-dialog")
        ) {
            Text(
                text = "Nonaktifkan pengguna?",
                style = ownerTextStyle(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$userName tidak akan lagi dapat login ke aplikasi sampai diaktifkan kembali.",
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
                        .testTag("detail-user-deactivate-cancel"),
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
                        .background(UserDangerColor)
                        .clickable(
                            enabled = !isBusy,
                            role = Role.Button,
                            onClick = onConfirm
                        )
                        .padding(horizontal = 20.dp)
                        .testTag("detail-user-deactivate-confirm"),
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

internal fun sampleDetailUser(isActive: Boolean = true): StaffMember = StaffMember(
    id = "u1",
    name = "Andi Pratama",
    email = "andi@tbterminal.id",
    rawEmail = "andi@tbterminal.id",
    username = "andi.admin",
    roleId = "r-admin",
    role = StaffRole.Admin,
    status = if (isActive) StaffStatus.Active else StaffStatus.Inactive,
    lastLogin = "15 menit lalu",
    lastLoginEpochMillis = 1_760_000_000_000L,
    initials = "AP"
)

@Preview(
    name = "OwnerDetailUserPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 880
)
@Composable
internal fun OwnerDetailUserPhonePreview() {
    TbterminalappTheme {
        OwnerDetailUserScreen(
            uiState = UserDetailUiState(
                staff = sampleDetailUser(isActive = true),
                isLoading = false
            ),
            onBack = {},
            onRetry = {},
            onEditClick = {},
            onChangePasswordClick = {},
            onChangePinClick = {},
            onCopyUsername = {},
            onDeactivateClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerDetailUserTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerDetailUserTabletPreview() {
    TbterminalappTheme {
        OwnerDetailUserScreen(
            uiState = UserDetailUiState(
                staff = sampleDetailUser(isActive = true),
                isLoading = false
            ),
            onBack = {},
            onRetry = {},
            onEditClick = {},
            onChangePasswordClick = {},
            onChangePinClick = {},
            onCopyUsername = {},
            onDeactivateClick = {},
            onNavSelect = {}
        )
    }
}
