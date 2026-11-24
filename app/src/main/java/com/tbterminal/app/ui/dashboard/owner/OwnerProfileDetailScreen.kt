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
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbterminal.app.TbTerminalApplication
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.settings.CredentialChangeDialog
import com.tbterminal.app.ui.settings.CredentialDialog
import com.tbterminal.app.ui.settings.ProfileViewModel
import kotlin.math.abs
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.FieldBorder
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.OwnerSyncDot
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.util.Locale

private val OWNER_PROFILE_TABLET_BREAKPOINT = 840.dp
private val ProfileDangerColor = Color(0xFFB3123F)
private val ProfileDangerBorderColor = Color(0xFFF5C6D3)

@Immutable
internal data class ProfileDetailUiState(
    val userName: String = "Pemilik Toko",
    val role: String = "OWNER",
    val email: String? = "owner@gmail.com",
    val userId: String = "KSR-646",
    val storeName: String = "Terminal Barokah Jaya",
    // TODO: Muat foto profil melingkar 64dp saat model UserProfile menyediakan field photoUrl.
    val photoUrl: String? = null,
    val synced: Boolean = true
) {
    val roleBadgeLabel: String
        get() = formatMenuRoleLabel(role)

    val roleInfoLabel: String
        get() = formatProfileAccountRoleLabel(role)

    val initials: String
        get() = formatProfileTwoLetterInitials(userName)
}

internal fun sampleReferenceProfileState(): ProfileDetailUiState = ProfileDetailUiState(
    userName = "Pemilik Toko",
    role = "OWNER",
    email = "owner@gmail.com",
    userId = "KSR-646",
    storeName = "Terminal Barokah Jaya",
    photoUrl = null,
    synced = true
)

internal fun formatProfileAccountRoleLabel(role: String?): String = formatMenuRoleLabel(role)

internal fun formatShortProfileUserId(
    rawId: String?,
    role: String?,
    username: String? = null
): String {
    val trimmed = rawId?.trim().orEmpty()
    val shortCodeRegex = Regex("^[A-Za-z]{2,5}-\\d{2,6}$")
    if (trimmed.matches(shortCodeRegex)) {
        return trimmed.uppercase(Locale.ROOT)
    }
    if (trimmed.isNotEmpty() && trimmed.length <= 12 && !trimmed.contains('-')) {
        return trimmed
    }
    val prefix = when (role?.trim()?.lowercase(Locale.ROOT)) {
        "owner" -> "OWN"
        "admin" -> "ADM"
        else -> "KSR"
    }
    val digits = trimmed.filter(Char::isDigit)
    val suffix = if (digits.length >= 3) {
        digits.takeLast(3)
    } else {
        val seed = (trimmed.ifEmpty { username?.trim().orEmpty() }).ifEmpty { "001" }
        val num = if (digits.isNotEmpty()) digits.toIntOrNull() ?: 1 else (abs(seed.hashCode()) % 900) + 100
        num.toString().padStart(3, '0').takeLast(3)
    }
    return "$prefix-$suffix"
}

internal fun resolveDisplayStoreName(candidate: String?): String? {
    val trimmed = candidate?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed.equals("TB TERMINAL", ignoreCase = true)) {
        return null
    }
    return trimmed
}

internal fun formatProfileTwoLetterInitials(name: String): String {
    val words = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase(Locale.ROOT)
        words.size == 1 -> words[0].take(2).uppercase(Locale.ROOT)
        else -> "PT"
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun OwnerProfileDetailRoute(
    fallbackName: String,
    fallbackRole: String,
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
    onEditProfile: (() -> Unit)?,
    onDashboardClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as? TbTerminalApplication)?.appContainer
    val destinationNavigator = LocalAdminDestinationNavigator.current

    val offlineStatusFlow = remember(appContainer) { appContainer?.offlineStatusRepository?.status }
    val offlineStatus by (
        offlineStatusFlow?.collectAsState(initial = OfflineStatus())
            ?: remember { mutableStateOf(OfflineStatus()) }
        )

    var storeName by remember { mutableStateOf("Terminal Barokah Jaya") }
    LaunchedEffect(appContainer) {
        val cachedName = runCatching {
            appContainer?.localAppSettingsDataSource?.getCachedStoreConfig()?.storeName
        }.getOrNull()
        resolveDisplayStoreName(cachedName)?.let { storeName = it }

        val remoteRes = runCatching { appContainer?.systemRepository?.getStoreSettings() }.getOrNull()
        if (remoteRes is com.tbterminal.app.data.remote.NetworkResult.Success) {
            resolveDisplayStoreName(remoteRes.data.data?.storeName)?.let { storeName = it }
        }
    }

    val isSynced = remember(offlineStatus) {
        offlineStatus.isInternetOnline &&
            offlineStatus.backendStatus != BackendStatus.UNREACHABLE &&
            offlineStatus.failedSyncCount == 0
    }

    var credentialDialog by remember { mutableStateOf<CredentialDialog?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var copiedToastVisible by remember { mutableStateOf(false) }
    var lastRetryAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            credentialDialog = null
            lastRetryAction = null
        }
    }

    BackHandler(onBack = onBack)

    val profile = uiState.profile
    val detailState = remember(profile, fallbackName, fallbackRole, storeName, isSynced) {
        val resolvedName = profile?.name?.takeIf { it.isNotBlank() }
            ?: fallbackName.ifBlank { "Pemilik Toko" }
        val resolvedRole = profile?.role?.takeIf { it.isNotBlank() } ?: fallbackRole
        val resolvedEmail = profile?.email?.trim()?.takeIf { it.isNotBlank() }
        val resolvedId = formatShortProfileUserId(
            rawId = profile?.id,
            role = resolvedRole,
            username = profile?.username
        )
        ProfileDetailUiState(
            userName = resolvedName,
            role = resolvedRole,
            email = resolvedEmail,
            userId = resolvedId,
            storeName = storeName,
            photoUrl = null,
            synced = isSynced
        )
    }

    fun handleCopyUserId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("ID pengguna", detailState.userId))
        copiedToastVisible = true
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
        DetailProfilScreen(
            state = detailState,
            onBack = onBack,
            onCopyUserId = ::handleCopyUserId,
            onEditProfileClick = { onEditProfile?.invoke() },
            onChangePasswordClick = {
                profileViewModel.clearMessage()
                credentialDialog = CredentialDialog.PASSWORD
            },
            onChangePinClick = {
                profileViewModel.clearMessage()
                credentialDialog = CredentialDialog.PIN
            },
            onLogoutClick = { showLogoutConfirm = true },
            onNavSelect = ::handleNavSelect
        )

        AppAlert(
            type = AlertType.Success,
            title = "ID pengguna disalin",
            message = detailState.userId,
            visible = copiedToastVisible,
            onDismiss = { copiedToastVisible = false },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("profile-copy-toast")
        )

        val feedbackMessage = uiState.error ?: uiState.message
        if (!copiedToastVisible && feedbackMessage != null && (credentialDialog == null || uiState.isNetworkError)) {
            AppAlert(
                type = if (uiState.error != null) AlertType.Error else AlertType.Success,
                title = when {
                    uiState.isNetworkError -> "Tidak dapat terhubung"
                    uiState.error != null -> "Gagal memperbarui akun"
                    else -> feedbackMessage
                },
                message = if (uiState.error != null && !uiState.isNetworkError) feedbackMessage else "",
                actionLabel = if (uiState.isNetworkError && lastRetryAction != null) "Coba lagi" else null,
                onAction = { lastRetryAction?.invoke() },
                visible = true,
                onDismiss = profileViewModel::clearMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            )
        }
    }

    if (showLogoutConfirm) {
        ProfileLogoutConfirmDialog(
            isBusy = uiState.isSaving,
            onDismiss = { if (!uiState.isSaving) showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            }
        )
    }

    credentialDialog?.let { type ->
        CredentialChangeDialog(
            type = type,
            isSaving = uiState.isSaving,
            errorMessage = if (uiState.isNetworkError) null else uiState.error,
            onInputChanged = profileViewModel::clearMessage,
            onDismiss = {
                if (!uiState.isSaving) {
                    credentialDialog = null
                    profileViewModel.clearMessage()
                }
            },
            onSubmit = { oldVal, newVal, confirmVal ->
                if (type == CredentialDialog.PASSWORD) {
                    lastRetryAction = { profileViewModel.changePassword(oldVal, newVal, confirmVal) }
                    profileViewModel.changePassword(oldVal, newVal, confirmVal)
                } else {
                    lastRetryAction = { profileViewModel.changePin(oldVal, newVal, confirmVal) }
                    profileViewModel.changePin(oldVal, newVal, confirmVal)
                }
            },
            onVerifyCurrentPin = profileViewModel::verifyCurrentPin,
            onResetPinWithPassword = profileViewModel::resetPinWithPassword,
            onForceLogout = onLogout
        )
    }
}

@Composable
internal fun DetailProfilScreen(
    state: ProfileDetailUiState,
    onBack: () -> Unit,
    onCopyUserId: () -> Unit,
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_PROFILE_TABLET_BREAKPOINT
        if (isTablet) {
            TabletDetailProfilLayout(
                state = state,
                onBack = onBack,
                onCopyUserId = onCopyUserId,
                onEditProfileClick = onEditProfileClick,
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick,
                onLogoutClick = onLogoutClick,
                onNavSelect = onNavSelect
            )
        } else {
            PhoneDetailProfilLayout(
                state = state,
                onBack = onBack,
                onCopyUserId = onCopyUserId,
                onEditProfileClick = onEditProfileClick,
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun PhoneDetailProfilLayout(
    state: ProfileDetailUiState,
    onBack: () -> Unit,
    onCopyUserId: () -> Unit,
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header hijau ponsel (padding: 44dp 20dp 56dp), tanpa bottom nav
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
                SecondaryTopBar(
                    title = "Detail profil",
                    onBack = onBack,
                    backTestTag = "profile-back-btn"
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileIdentityHeader(state = state)
            }
        }

        // 2. Panel putih menumpuk 28dp ke atas header hijau (sudut atas radius 32dp)
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
            DetailProfilBody(
                state = state,
                onCopyUserId = onCopyUserId,
                onEditProfileClick = onEditProfileClick,
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun TabletDetailProfilLayout(
    state: ProfileDetailUiState,
    onBack: () -> Unit,
    onCopyUserId: () -> Unit,
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // 1. Rel navigasi samping 88dp tetap tampil, Menu aktif
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        // 2 & 3. Panel hijau 432dp + Panel putih menumpuk 32dp ke kiri
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
                    SecondaryTopBar(
                        title = "Detail profil",
                        onBack = onBack,
                        backTestTag = "profile-back-btn"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        ProfileIdentityHeader(state = state)
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
                DetailProfilBody(
                    state = state,
                    onCopyUserId = onCopyUserId,
                    onEditProfileClick = onEditProfileClick,
                    onChangePasswordClick = onChangePasswordClick,
                    onChangePinClick = onChangePinClick,
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }
}

@Composable
private fun ProfileIdentityHeader(
    state: ProfileDetailUiState,
    modifier: Modifier = Modifier
) {
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
                    text = state.initials,
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
                    .background(if (state.synced) OwnerSyncDot else FieldBorder)
                    .border(width = 2.dp, color = Brand, shape = CircleShape)
                    .testTag("profile-sync-dot")
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.userName,
                style = ownerTextStyle(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBrand,
                    letterSpacing = (-0.01).em
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .cssLineBoxHeight(26.dp)
                    .testTag("profile-detail-name")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OwnerRoleBadgePill(
                    roleLabel = state.roleBadgeLabel,
                    modifier = Modifier.testTag("profile-role-badge")
                )

                val emailText = state.email
                if (!emailText.isNullOrBlank()) {
                    Text(
                        text = emailText,
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = BrandSoftText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .cssLineBoxHeight(20.dp)
                            .testTag("profile-detail-email")
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailProfilBody(
    state: ProfileDetailUiState,
    onCopyUserId: () -> Unit,
    onEditProfileClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val securityGroup = remember(onEditProfileClick, onChangePasswordClick, onChangePinClick) {
        OwnerPlainMenuGroup(
            title = "Keamanan",
            items = listOf(
                OwnerPlainMenuItem(
                    tag = "profile-menu-edit",
                    title = "Ubah informasi profil",
                    subtitle = "Nama, email, dan foto",
                    icon = OwnerIcons.UserSingle,
                    iconTint = Brand,
                    onClick = onEditProfileClick
                ),
                OwnerPlainMenuItem(
                    tag = "profile-menu-password",
                    title = "Ubah kata sandi",
                    subtitle = null,
                    icon = OwnerIcons.LockClosed,
                    iconTint = Brand,
                    onClick = onChangePasswordClick
                ),
                OwnerPlainMenuItem(
                    tag = "profile-menu-pin",
                    title = "Ubah PIN transaksi",
                    subtitle = "PIN 6 digit untuk konfirmasi",
                    icon = OwnerIcons.PinDots,
                    iconTint = Brand,
                    onClick = onChangePinClick
                )
            )
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Info akun",
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
            label = "ID pengguna",
            value = state.userId,
            tabularValue = true,
            onCopyClick = onCopyUserId,
            copyContentDescription = "Salin id pengguna",
            copyTestTag = "profile-copy-id-btn",
            valueTestTag = "profile-user-id"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )
        InfoRow(
            label = "Peran",
            value = state.roleInfoLabel,
            tabularValue = true,
            valueTestTag = "profile-role-value"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OwnerDivider)
        )
        InfoRow(
            label = "Toko",
            value = state.storeName,
            tabularValue = true,
            valueTestTag = "profile-store-value"
        )

        Spacer(modifier = Modifier.height(32.dp))

        OwnerPlainMenuSection(group = securityGroup)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceWhite)
                .border(1.dp, ProfileDangerBorderColor, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onLogoutClick
                )
                .testTag("profile-logout-btn"),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.Logout,
                tint = ProfileDangerColor,
                size = 20.dp
            )
            Text(
                text = "Keluar",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileDangerColor
                ),
                maxLines = 1,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
        }
    }
}

@Composable
internal fun ProfileLogoutConfirmDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp)
                .testTag("profile-logout-dialog")
        ) {
            Text(
                text = "Keluar dari akun?",
                style = ownerTextStyle(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kamu perlu masuk lagi dengan nama pengguna dan kata sandi.",
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
                        .testTag("profile-logout-cancel"),
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
                        .background(ProfileDangerColor)
                        .clickable(
                            enabled = !isBusy,
                            role = Role.Button,
                            onClick = onConfirm
                        )
                        .padding(horizontal = 20.dp)
                        .testTag("account-confirm-logout"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keluar",
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

@Preview(
    name = "DetailProfilPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 880
)
@Composable
internal fun DetailProfilPhonePreview() {
    TbterminalappTheme {
        DetailProfilScreen(
            state = sampleReferenceProfileState(),
            onBack = {},
            onCopyUserId = {},
            onEditProfileClick = {},
            onChangePasswordClick = {},
            onChangePinClick = {},
            onLogoutClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "DetailProfilTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun DetailProfilTabletPreview() {
    TbterminalappTheme {
        DetailProfilScreen(
            state = sampleReferenceProfileState(),
            onBack = {},
            onCopyUserId = {},
            onEditProfileClick = {},
            onChangePasswordClick = {},
            onChangePinClick = {},
            onLogoutClick = {},
            onNavSelect = {}
        )
    }
}
