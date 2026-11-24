package com.tbterminal.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.dashboard.owner.DashboardNavDestination
import com.tbterminal.app.ui.dashboard.owner.OwnerRoleBadgePill
import com.tbterminal.app.ui.dashboard.owner.OwnerTabletNavRail
import com.tbterminal.app.ui.dashboard.owner.SecondaryTopBar
import com.tbterminal.app.ui.dashboard.owner.cssLineBoxHeight
import com.tbterminal.app.ui.dashboard.owner.formatMenuRoleLabel
import com.tbterminal.app.ui.dashboard.owner.formatProfileTwoLetterInitials
import com.tbterminal.app.ui.dashboard.owner.ownerTextStyle
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val EDIT_PROFILE_TABLET_BREAKPOINT = 840.dp
private val USERNAME_REGEX = Regex("^[a-zA-Z0-9._-]{3,50}$")
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Suppress("UNUSED_PARAMETER")
@Composable
fun SharedEditProfileScreen(
    uiState: ProfileUiState,
    onSave: (String, String, String?) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    fallbackName: String = "",
    fallbackRole: String = "",
    onNavSelect: ((DashboardNavDestination) -> Unit)? = null,
) {
    val profile = uiState.profile
    val profileSignature = remember(profile?.id, profile?.username, profile?.name, profile?.email) {
        listOf(
            profile?.id.orEmpty(),
            profile?.username.orEmpty(),
            profile?.name.orEmpty(),
            profile?.email.orEmpty()
        ).joinToString("|")
    }

    var loadedSignature by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf(profile?.name ?: fallbackName) }
    var username by rememberSaveable { mutableStateOf(profile?.username.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(profile?.email.orEmpty()) }
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    var usernameTouched by rememberSaveable { mutableStateOf(false) }
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var infoBannerMessage by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profileSignature, profile) {
        if (profile != null && loadedSignature != profileSignature) {
            val prevBaselineEmpty = loadedSignature.isEmpty()
            if (prevBaselineEmpty || !nameTouched) {
                name = profile.name
            }
            if (prevBaselineEmpty || !usernameTouched) {
                username = profile.username
            }
            if (prevBaselineEmpty || !emailTouched) {
                email = profile.email.orEmpty()
            }
            loadedSignature = profileSignature
        }
    }

    LaunchedEffect(uiState.profileUpdated) {
        if (uiState.profileUpdated) {
            onSaved()
        }
    }

    val trimmedName = name.trim()
    val trimmedUsername = username.trim()
    val normalizedEmail = email.trim().ifBlank { null }

    val hasChanges = remember(profile, trimmedName, trimmedUsername, normalizedEmail) {
        if (profile == null) {
            false
        } else {
            trimmedName != profile.name ||
                trimmedUsername != profile.username ||
                normalizedEmail != profile.email?.trim()?.ifBlank { null }
        }
    }

    val validationError = remember(trimmedName, trimmedUsername, normalizedEmail) {
        validateProfileForm(trimmedName, trimmedUsername, normalizedEmail)
    }

    val handleBack: () -> Unit = {
        if (!uiState.isSaving) {
            if (hasChanges) {
                showDiscardDialog = true
            } else {
                onBack()
            }
        }
    }

    BackHandler(enabled = !uiState.isSaving, onBack = handleBack)

    val resolvedRole = profile?.role?.takeIf { it.isNotBlank() } ?: fallbackRole
    val resolvedRoleLabel = remember(resolvedRole) { formatMenuRoleLabel(resolvedRole) }
    val previewDisplayName = trimmedName.ifBlank { profile?.name ?: fallbackName.ifBlank { "Pemilik Toko" } }
    val previewUsername = trimmedUsername.ifBlank { profile?.username ?: "username" }
    val initials = remember(previewDisplayName) { formatProfileTwoLetterInitials(previewDisplayName) }

    val focusManager = LocalFocusManager.current

    fun submitForm() {
        nameTouched = true
        usernameTouched = true
        emailTouched = true
        if (!uiState.isSaving && validationError == null && hasChanges) {
            onClearMessage()
            onSave(trimmedName, trimmedUsername, normalizedEmail)
        }
    }

    val formBody: @Composable () -> Unit = {
        if (uiState.isLoading && profile == null) {
            EditProfileFormSkeleton()
        } else if (profile != null) {
            EditProfileFormContent(
                name = name,
                username = username,
                email = email,
                nameTouched = nameTouched,
                usernameTouched = usernameTouched,
                emailTouched = emailTouched,
                backendError = uiState.error,
                isSaving = uiState.isSaving,
                canSubmit = !uiState.isSaving && validationError == null && hasChanges,
                onNameChange = {
                    nameTouched = true
                    name = it.take(100)
                    onClearMessage()
                },
                onUsernameChange = { raw ->
                    usernameTouched = true
                    username = raw.filterNot(Char::isWhitespace).take(50)
                    onClearMessage()
                },
                onEmailChange = { raw ->
                    emailTouched = true
                    email = raw.filterNot(Char::isWhitespace).take(150)
                    onClearMessage()
                },
                onNextFocus = { focusManager.moveFocus(FocusDirection.Down) },
                onSubmit = ::submitForm,
                onCancel = handleBack
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Data profil tidak tersedia",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                )
                Text(
                    text = uiState.error ?: "Periksa koneksi internet Anda lalu coba kembali.",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Muted
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= EDIT_PROFILE_TABLET_BREAKPOINT
            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceWhite)
                ) {
                    if (onNavSelect != null) {
                        OwnerTabletNavRail(
                            activeDestination = DashboardNavDestination.MENU,
                            onNavSelect = onNavSelect
                        )
                    }

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
                                    title = "Ubah informasi profil",
                                    onBack = handleBack,
                                    backTestTag = "edit-profile-back-btn"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    EditProfileIdentitySummary(
                                        initials = initials,
                                        displayName = previewDisplayName,
                                        roleLabel = resolvedRoleLabel,
                                        username = previewUsername,
                                        onAvatarClick = {
                                            infoBannerMessage = "Unggah foto profil akan hadir pada pembaruan berikutnya."
                                        }
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
                                .imePadding()
                                .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp)
                        ) {
                            formBody()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceWhite)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
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
                            SecondaryTopBar(
                                title = "Ubah informasi profil",
                                onBack = handleBack,
                                backTestTag = "edit-profile-back-btn"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            EditProfileIdentitySummary(
                                initials = initials,
                                displayName = previewDisplayName,
                                roleLabel = resolvedRoleLabel,
                                username = previewUsername,
                                onAvatarClick = {
                                    infoBannerMessage = "Unggah foto profil akan hadir pada pembaruan berikutnya."
                                }
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
                        formBody()
                    }
                }
            }
        }

        val activeAlertText = uiState.error ?: infoBannerMessage ?: uiState.message
        if (activeAlertText != null) {
            val isErrorAlert = uiState.error != null
            AppAlert(
                type = when {
                    isErrorAlert -> AlertType.Error
                    infoBannerMessage != null -> AlertType.Warning
                    else -> AlertType.Success
                },
                title = when {
                    isErrorAlert -> "Gagal menyimpan perubahan"
                    infoBannerMessage != null -> "Foto profil"
                    else -> "Berhasil"
                },
                message = activeAlertText,
                visible = true,
                onDismiss = {
                    infoBannerMessage = null
                    onClearMessage()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .testTag("edit-profile-alert")
            )
        }
    }

    if (showDiscardDialog) {
        Dialog(onDismissRequest = { showDiscardDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceWhite)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Batalkan perubahan?",
                    style = ownerTextStyle(
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Perubahan profil yang belum disimpan akan hilang.",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
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
                            .clickable(role = Role.Button, onClick = { showDiscardDialog = false })
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lanjut edit",
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
                            .background(FieldErrorColor)
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    showDiscardDialog = false
                                    onBack()
                                }
                            )
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Buang",
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
}

@Composable
private fun EditProfileIdentitySummary(
    initials: String,
    displayName: String,
    roleLabel: String,
    username: String,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onAvatarClick
                )
                .semantics { contentDescription = "Ubah foto profil" }
                .testTag("edit-profile-avatar-btn")
        ) {
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
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .border(2.dp, Brand, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = Brand,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
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
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OwnerRoleBadgePill(roleLabel = roleLabel)
                Text(
                    text = "@$username",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = BrandSoftText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .cssLineBoxHeight(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EditProfileFormContent(
    name: String,
    username: String,
    email: String,
    nameTouched: Boolean,
    usernameTouched: Boolean,
    emailTouched: Boolean,
    backendError: String?,
    isSaving: Boolean,
    canSubmit: Boolean,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onNextFocus: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    val nameError = when {
        nameTouched && name.trim().length < 2 -> "Nama lengkap minimal 2 karakter"
        backendError?.contains("nama", ignoreCase = true) == true -> backendError
        else -> null
    }
    val usernameError = when {
        usernameTouched && !username.trim().matches(USERNAME_REGEX) ->
            "Username 3–50 karakter (huruf, angka, ., _, - tanpa spasi)"
        backendError?.contains("username", ignoreCase = true) == true -> backendError
        else -> null
    }
    val emailError = when {
        emailTouched && email.isNotBlank() && !email.trim().matches(EMAIL_REGEX) ->
            "Format alamat email tidak valid"
        backendError?.contains("email", ignoreCase = true) == true -> backendError
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("edit-profile-form")
    ) {
        Text(
            text = "Data identitas & akun",
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .cssLineBoxHeight(20.dp)
                .semantics { heading() }
        )

        ProfileOutlinedInputField(
            label = "Nama lengkap",
            value = name,
            onValueChange = onNameChange,
            enabled = !isSaving,
            placeholder = "Masukkan nama lengkap",
            leadingIcon = Icons.Outlined.Person,
            errorText = nameError,
            helperText = "Nama tampil di profil, laporan, dan struk transaksi",
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onNextFocus() }),
            testTag = "edit-profile-name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileOutlinedInputField(
            label = "Username",
            value = username,
            onValueChange = onUsernameChange,
            enabled = !isSaving,
            placeholder = "mis. owner.barokah",
            leadingIcon = Icons.Outlined.AlternateEmail,
            errorText = usernameError,
            helperText = "Digunakan untuk masuk ke aplikasi (huruf, angka, . _ -)",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onNextFocus() }),
            testTag = "edit-profile-username"
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileOutlinedInputField(
            label = "Email (opsional)",
            value = email,
            onValueChange = onEmailChange,
            enabled = !isSaving,
            placeholder = "nama@toko.com",
            leadingIcon = Icons.Outlined.MailOutline,
            errorText = emailError,
            helperText = "Digunakan untuk kontak akun dan informasi sistem",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            testTag = "edit-profile-email"
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (canSubmit) Brand else OwnerBorder)
                .clickable(
                    enabled = canSubmit,
                    role = Role.Button,
                    onClick = onSubmit
                )
                .testTag("edit-profile-save"),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = OnBrand,
                    strokeWidth = 2.dp
                )
            }
            Text(
                text = if (isSaving) "Menyimpan..." else "Simpan perubahan",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canSubmit || isSaving) OnBrand else Muted
                ),
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceWhite)
                .border(1.dp, OwnerBorder, RoundedCornerShape(16.dp))
                .clickable(
                    enabled = !isSaving,
                    role = Role.Button,
                    onClick = onCancel
                )
                .testTag("edit-profile-cancel"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Batal",
                style = ownerTextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
        }
    }
}

@Composable
private fun ProfileOutlinedInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
    leadingIcon: ImageVector,
    errorText: String?,
    helperText: String?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isError = errorText != null

    val borderColor = when {
        isError -> FieldErrorColor
        isFocused -> Brand
        else -> OwnerBorder
    }
    val borderWidth = if (isFocused || isError) 1.5.dp else 1.dp
    val fieldBg = when {
        !enabled -> ChipBg
        isFocused -> SurfaceWhite
        else -> SurfaceWhite
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(fieldBg)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = when {
                    isError -> FieldErrorColor
                    isFocused -> Brand
                    else -> Muted
                },
                modifier = Modifier.size(20.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = ownerTextStyle(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = Muted.copy(alpha = 0.65f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = ownerTextStyle(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(if (isError) FieldErrorColor else Brand),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }

            if (value.isNotEmpty() && enabled) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = { onValueChange("") }
                        )
                        .semantics { contentDescription = "Hapus $label" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        val supporting = errorText ?: helperText
        if (supporting != null) {
            Text(
                text = supporting,
                style = ownerTextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (isError) FontWeight.Medium else FontWeight.Normal,
                    color = if (isError) FieldErrorColor else Muted
                )
            )
        }
    }
}

@Composable
private fun EditProfileFormSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBox(modifier = Modifier.width(160.dp).height(20.dp), shape = RoundedCornerShape(6.dp))
        repeat(3) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp))
        }
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp))
    }
}

@Preview(name = "EditProfilPhone", showBackground = true, widthDp = 390, heightDp = 880)
@Composable
internal fun EditProfilPhonePreview() {
    TbterminalappTheme {
        SharedEditProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                profile = UserProfile(
                    id = "KSR-646",
                    username = "owner.barokah",
                    name = "Pemilik Toko",
                    role = "OWNER",
                    email = "owner@gmail.com",
                    isActive = true,
                    joinedAt = "2026-01-01",
                    lastLoginAt = null
                )
            ),
            onSave = { _, _, _ -> },
            onBack = {},
            onSaved = {},
            onClearMessage = {}
        )
    }
}

@Preview(name = "EditProfilTablet", showBackground = true, widthDp = 1024, heightDp = 768)
@Composable
internal fun EditProfilTabletPreview() {
    TbterminalappTheme {
        SharedEditProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                profile = UserProfile(
                    id = "KSR-646",
                    username = "owner.barokah",
                    name = "Pemilik Toko",
                    role = "OWNER",
                    email = "owner@gmail.com",
                    isActive = true,
                    joinedAt = "2026-01-01",
                    lastLoginAt = null
                )
            ),
            onSave = { _, _, _ -> },
            onBack = {},
            onSaved = {},
            onClearMessage = {},
            onNavSelect = {}
        )
    }
}
